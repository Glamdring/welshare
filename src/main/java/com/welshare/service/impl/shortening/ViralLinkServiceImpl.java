package com.welshare.service.impl.shortening;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.welshare.dao.ViralLinkDao;
import com.welshare.model.ShortUrl;
import com.welshare.model.ShortenedLinkVisitData;
import com.welshare.model.ViralShortUrl;
import com.welshare.service.ViralLinkService;

@Service
public class ViralLinkServiceImpl implements ViralLinkService {

    private static final Logger logger = LoggerFactory.getLogger(ViralLinkServiceImpl.class);

    @Inject
    private ViralLinkDao dao;

    @Override
    public ShortUrl followViralLink(String key, String currentUserId, ShortenedLinkVisitData data) {
        return dao.spawnLink(key, currentUserId, data);
    }

    @Override
    public void getViralGraphImage(String key, OutputStream out) {
        try {
            int width = 800;
            int height = 800;
            Image singleUserIcon = ImageIO.read(getClass().getResourceAsStream("/images/user.png"));
            Image multipleUsersIcon = ImageIO.read(getClass().getResourceAsStream("/images/users.png"));

            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            Graphics2D g2 = (Graphics2D) img.getGraphics();
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(20.0f, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER));

            int centerX = width / 2 - singleUserIcon.getWidth(null) / 2;
            g2.drawImage(singleUserIcon, centerX, 30, null);

            ViralShortUrl url = dao.getLink(key);
            if (url.getNodesFromBeginning() == 1 || url.getNodesFromBeginning() == 2) {
                g2.drawImage(singleUserIcon, width / 2 - 50, 100, null);
                g2.drawLine(centerX, 30 + singleUserIcon.getHeight(null) + 5, width / 2 - 50, 100 - 5);
            } else if (url.getNodesFromBeginning() > 2) {
                g2.drawImage(multipleUsersIcon, width / 2 - 50, 100, null);
                g2.drawLine(centerX, 30 + singleUserIcon.getHeight(null) + 5, width / 2 - 50, 100 - 5);
                // TODO draw number and other dummy nodes
            }



        } catch (IOException ex) {
            logger.error("Cannot create viral graph image", ex);
            try {
                IOUtils.copy(getClass().getResourceAsStream("/images/emptyViralLinkImage.png"), out);
            } catch (IOException e) {
                logger.error("Cannot send viral graph placeholder image", ex);
            }
        }

    }

    @Override
    public ViralShortUrl getViralUrl(String key) {
        return dao.getLink(key);
    }

}
