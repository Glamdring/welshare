package com.welshare.graph;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.index.lucene.LuceneIndexService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.springframework.data.graph.neo4j.support.DelegatingGraphDatabase;
import org.springframework.data.graph.neo4j.template.Neo4jTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.welshare.dao.ViralLinkDao;
import com.welshare.dao.neo4j.ViralLinkDaoNeo4j;
import com.welshare.model.ShortUrl;
import com.welshare.model.ShortenedLinkVisitData;
import com.welshare.model.User;
import com.welshare.model.ViralShortUrl;

public class ViralLinkTest {
    private static final int MAX_LEVELS = 5;
    private static final int CHILDREN_PER_NODE = 4;

    private ViralLinkDao dao = new ViralLinkDaoNeo4j();

    private int currentUserId = 2;

    private List<ViralShortUrl> urls = new ArrayList<ViralShortUrl>();

    @Before
    public void setUp() throws Exception {
        String dir = System.getProperty("java.io.tmpdir") + "/neo4jtest";

        FileUtils.deleteDirectory(new File(dir));
        GraphDatabaseService db = new EmbeddedGraphDatabase(dir);

        ReflectionTestUtils.setField(dao, "db", db);
        ReflectionTestUtils.setField(dao, "index", new LuceneIndexService(db));
        ReflectionTestUtils.setField(dao, "template", new Neo4jTemplate(new DelegatingGraphDatabase(db)));
    }

    @Test
    public void viralLinkTest() throws Exception {
        ShortenedLinkVisitData data = new ShortenedLinkVisitData();
        ShortUrl original = new ShortUrl();
        original.setKey("abcdefg");
        original.setLongUrl("http://google.com");
        original.setTrackViral(true);
        User user = new User();
        user.setId("user0");
        original.setUser(user);
        dao.storeShortUrl(original);

        ShortUrl url = dao.spawnLink(original.getKey(), "user1", data);
        spawnUrls(url, 0, data); // this recursive method verifies nodes-from-beginning

        for (ViralShortUrl vurl : urls) {
            vurl = dao.getLink(vurl.getKey());
            int expected = 0;
            for (int i = 0; i <= MAX_LEVELS - vurl.getNodesFromBeginning(); i++) {
                expected += Math.pow(CHILDREN_PER_NODE, i);
            }
            expected--; //not counting start node

            Assert.assertEquals("Node with key " + vurl.getKey() + " has incorrectly calculated viral points",
                expected, vurl.getViralPoints());

            Assert.assertEquals("Incorrect average depth of subgraph for node with key" + vurl.getKey(),
                MAX_LEVELS - vurl.getNodesFromBeginning(), vurl.getAverageSubgraphDepth());
        }
    }

    private void spawnUrls(ShortUrl url, int step, ShortenedLinkVisitData data) {
        step++;
        if (step >= MAX_LEVELS) {
            return;
        }
        for (int i = 0; i < CHILDREN_PER_NODE; i++) {
            ViralShortUrl newUrl = dao.spawnLink(url.getKey(), "user" + currentUserId, data);
            urls.add(newUrl);
            Assert.assertEquals(step + 1, newUrl.getNodesFromBeginning());
            currentUserId++;
            spawnUrls(newUrl, step, data);
        }
    }
}
