package com.welshare.graph;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.index.IndexService;
import org.neo4j.index.lucene.LuceneIndexService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.springframework.data.graph.neo4j.support.DelegatingGraphDatabase;
import org.springframework.data.graph.neo4j.template.Neo4jTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.welshare.dao.FollowingDao;
import com.welshare.dao.neo4j.FollowingDaoNeo4j;
import com.welshare.model.Following;
import com.welshare.model.User;

@Ignore
public class FollowingTest {

    @Inject
    private FollowingDao dao = new FollowingDaoNeo4j();

    private GraphDatabaseService db;

    private IndexService index;

    private Neo4jTemplate template;

    @Before
    public void setUp() throws Exception {
        String dir = System.getProperty("java.io.tmpdir") + "/neo4jtest";
        System.out.println(dir);
        //FileUtils.deleteDirectory(new File(dir));
        db = new EmbeddedGraphDatabase(dir);
        index = new LuceneIndexService(db);

        template = new Neo4jTemplate(new DelegatingGraphDatabase(db));
        ReflectionTestUtils.setField(dao, "template", template);
        ReflectionTestUtils.setField(dao, "index", index);
    }

    @Test
    public void followingTest() throws Exception {
        Transaction tx = db.beginTx();
        User user = new User();
        user.setId("foo");
        user.setUsername("ufoo");
        User followed = new User();
        followed.setId("bar");
        followed.setUsername("ubar");
        Following f = new Following();
        f.setFollower(user);
        f.setFollowed(followed);
        try {
            dao.saveFollowing(f);
            tx.success();
        } catch (Exception ex) {
            tx.failure();
            throw ex;
        } finally {
            tx.finish();
        }
    }
}
