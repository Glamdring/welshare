package com.welshare.service;

import javax.inject.Inject;
import javax.validation.ConstraintViolationException;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionSystemException;

import com.welshare.model.User;
import com.welshare.model.enums.Language;
import com.welshare.service.exception.UserException;
import com.welshare.test.BaseSpringTest;

public class UserServiceTest extends BaseSpringTest {

    @Inject
    private UserService userService;

    @Test
    public void registrationTest() {
        User user = new User();
        user.setUsername("fooo");
        user.setPassword("1234");
        user.getProfile().setLanguage(Language.EN);
        user.setEmail("foo@bar.com");

        try {
            userService.register(user);
            Assert.fail(); // should throw an exception because names are missing
        } catch (UserException e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        } catch (TransactionSystemException e) {
            ConstraintViolationException ex = (ConstraintViolationException) e.getMostSpecificCause();
            Assert.assertEquals("names", ex.getConstraintViolations()
                    .iterator().next().getPropertyPath().iterator().next()
                    .getName());
            // expected, continue
        }

        user.setNames("foo bar");

        try {
            user = userService.register(user);
            Assert.assertNotNull(user.getId());
        } catch (UserException e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
    }
}
