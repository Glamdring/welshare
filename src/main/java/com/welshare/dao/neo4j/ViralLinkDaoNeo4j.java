package com.welshare.dao.neo4j;

import java.util.Iterator;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.index.IndexService;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.graph.core.Property;
import org.springframework.data.graph.neo4j.template.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import com.welshare.dao.ViralLinkDao;
import com.welshare.model.ShortUrl;
import com.welshare.model.ShortenedLinkVisitData;
import com.welshare.model.ViralShortUrl;
import com.welshare.util.Constants;
import com.welshare.util.GeneralUtils;

@Repository
public class ViralLinkDaoNeo4j implements ViralLinkDao {

    private static final String CREATED = "created";

    private static final Logger logger = LoggerFactory.getLogger(ViralLinkDaoNeo4j.class);

    private static final String SESSION_ID = "sessionId";
    private static final String USER_ID = "userId";
    private static final String LONG_URL = "longUrl";
    private static final String INITIAL_KEY = "initialKey";
    private static final String KEY = "key";
    private static final String USER_AND_INITIAL_KEY = "userAndInitialKey";

    @Inject
    private Neo4jTemplate template;

    // TODO use declarative transactions
    @Inject
    private GraphDatabaseService db;

    @Inject
    private IndexService index;

    @Override
    public ViralShortUrl spawnLink(String originalKey, String userId, ShortenedLinkVisitData data) {
        Transaction tx = db.beginTx();
        String newKey = null;
        ViralShortUrl url = new ViralShortUrl();
        try {
            Node originalNode = index.getSingleNode(KEY, originalKey);
            if (originalNode == null) {
                return null;
            }
            String initialKey = null;
            if (originalNode.hasProperty(INITIAL_KEY)) {
                initialKey = (String) originalNode.getProperty(INITIAL_KEY);
            } else {
                initialKey = originalKey;
            }

            Node initialNode = index.getSingleNode(KEY, initialKey);
            url.setLongUrl((String) initialNode.getProperty(LONG_URL));
            url.setShowTopBar(true);
            url.setTrackViral(true);

            // if this is the user that owns (has created) the link,
            // do not spawn a new link, but return the current one instead
            if ((userId != null && originalNode.hasProperty(USER_ID) && userId.equals(originalNode.getProperty(USER_ID)))
                    || (originalNode.hasProperty(SESSION_ID) && data.getSessionId().equals(originalNode.getProperty(SESSION_ID)))) {
                url.setKey(originalKey);
                fillUrlData(originalNode, url);
                return url;
            }
            Node newNode = null;

            // first look for an existing node - i.e. the current user has
            // already visited the link that was sent to him again by another
            // user
            if (userId != null) {
                newNode = index.getSingleNode(USER_AND_INITIAL_KEY, userId + ":" + initialKey);
                if (newNode != null) {
                    logger.debug("Found existing node for user_and_initial_key=" + userId + ":" + initialKey);
                    newKey = (String) newNode.getProperty(KEY);
                }
            }

            if (newNode == null) {
                // generate a new key (and do so until it's unique in the graph)
                newKey = initialKey + GeneralUtils.generateShortKey(Constants.VIRAL_URL_ADDITION_LENGTH);
                while (index.getSingleNode(KEY, newKey) != null) {
                    newKey = initialKey + GeneralUtils.generateShortKey(Constants.VIRAL_URL_ADDITION_LENGTH);
                }
                newNode = template.createNode(new Property(KEY, newKey));
                logger.debug("Created new node with key: " + newKey + " for user " + userId);
                newNode.setProperty(INITIAL_KEY, initialKey);
                if (userId != null) {
                    newNode.setProperty(USER_ID, userId);
                }
                if (data.getSessionId() != null) {
                    newNode.setProperty(SESSION_ID, data.getSessionId());
                }
                if (data.getReferer() != null) {
                    newNode.setProperty("referer", data.getReferer());
                }
                if (data.getLanguage() != null) {
                    newNode.setProperty("language", data.getLanguage());
                }
                if (data.getIp() != null) {
                    newNode.setProperty("ip", data.getIp());
                }

                index.index(newNode, KEY, newKey);
                if (userId != null) {
                    index.index(newNode, USER_AND_INITIAL_KEY, userId + ":" + initialKey);
                }
            }
            Relationship relationship = originalNode.createRelationshipTo(newNode, LinkRelationship.SPAWNS);
            relationship.setProperty(CREATED, DateTimeUtils.currentTimeMillis());

            fillNodesFromBeginning(newNode, url);
            tx.success();
        } catch (Exception ex) {
            logger.error("Problem with spawning a new link for key " + originalKey, ex);
            tx.failure();
        } finally {
            tx.finish();
        }
        url.setKey(newKey);

        return url;
    }

    private void fillUrlData(Node currentNode, ViralShortUrl url) {

        fillNodesFromBeginning(currentNode, url);

        // counting all nodes from this one to the end of the graph,
        // in the outgoing direction. These are the viral points
        Traverser t = Traversal.description().breadthFirst()
            .uniqueness(Uniqueness.NODE_PATH)
            .evaluator(Evaluators.excludeStartPosition())
            .relationships(LinkRelationship.SPAWNS, Direction.OUTGOING)
            .traverse(currentNode);

        url.setViralPoints(IteratorUtil.count(t));

        // getting the average depth the subgraph of this node
        // (in the outgoing direction)
        AverageDepthEvaluator evaluator = new AverageDepthEvaluator();
        t = Traversal.description().depthFirst()
            .uniqueness(Uniqueness.NODE_PATH)
            .evaluator(evaluator)
            .relationships(LinkRelationship.SPAWNS, Direction.OUTGOING)
            .traverse(currentNode);
        // just loop everything so that the calculations take place
        IteratorUtil.lastOrNull(t);

        url.setAverageSubgraphDepth(evaluator.getAverageDepth());


        // count all nodes and calculate viral point percentage for this link
        Node initialNode = currentNode;
        if (currentNode.hasProperty(INITIAL_KEY)) {
            initialNode = index.getSingleNode(KEY, currentNode.getProperty(INITIAL_KEY));
        }

        t = Traversal.description().breadthFirst()
            .relationships(LinkRelationship.SPAWNS, Direction.BOTH)
            .evaluator(Evaluators.all())
            .uniqueness(Uniqueness.NODE_GLOBAL)
            .traverse(initialNode);

        double total = IteratorUtil.count(t);
        url.setViralPointsPercentage(url.getViralPoints() / total);
    }

    private void fillNodesFromBeginning(Node currentNode, ViralShortUrl url) {
        // nodes from beginning, counted via a simple depth-first traversal
        // in the opposite direction
        Traverser t = Traversal.description().depthFirst()
            .evaluator(Evaluators.excludeStartPosition())
            .relationships(LinkRelationship.SPAWNS, Direction.INCOMING)
            .traverse(currentNode);
        int nodesFromBeginning = IteratorUtil.count(t);
        url.setNodesFromBeginning(nodesFromBeginning);
    }


    @Override
    public void storeShortUrl(ShortUrl shortUrl) {
        Transaction tx = db.beginTx();
        try {
            Node node = template.createNode(new Property(KEY, shortUrl.getKey()));
            node.setProperty(LONG_URL, shortUrl.getLongUrl());
            node.setProperty(USER_ID, shortUrl.getUser().getId());
            index.index(node, KEY, shortUrl.getKey());
            tx.success();
        } catch (Exception ex) {
            logger.error("Problem shortening url in the graph", ex);
            tx.failure();
        } finally {
            tx.finish();
        }

    }

    @Override
    public ViralShortUrl getLink(String key) {
        Node node = index.getSingleNode(KEY, key);
        if (node == null) {
            return null;
        }
        ViralShortUrl url = new ViralShortUrl();
        if (node.hasProperty(INITIAL_KEY)) {
            Node initialNode = index.getSingleNode(KEY, node.getProperty(INITIAL_KEY));
            url.setLongUrl((String) initialNode.getProperty(LONG_URL));
        } else {
            url.setLongUrl((String) node.getProperty(LONG_URL));
        }
        url.setKey(key);
        url.setShowTopBar(true);
        url.setTrackViral(true);
        Iterator<Relationship> relationshipIterator = node.getRelationships(LinkRelationship.SPAWNS, Direction.INCOMING).iterator();
        if (relationshipIterator.hasNext()) {
            url.setTimeAdded(new DateTime(relationshipIterator.next().getProperty(CREATED)));
        }

        fillUrlData(node, url);
        return url;
    }

    private static enum LinkRelationship implements RelationshipType {
        SPAWNS
    }

    private static class AverageDepthEvaluator implements Evaluator {
        private int depthSum;
        private int leaves;

        @Override
        public Evaluation evaluate(Path path) {
            if (!path.endNode().hasRelationship(Direction.OUTGOING)) {
                leaves++;
                depthSum += path.length();
            }

            return Evaluation.EXCLUDE_AND_CONTINUE;
        }

        public int getAverageDepth() {
            if (leaves == 0) {
                return 0;
            }
            return depthSum / leaves;
        }
    }
}
