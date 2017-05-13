package aleks.kuzko.controller;

import aleks.kuzko.dao.PhraseDao;
import aleks.kuzko.dao.UserDao;
import aleks.kuzko.datamodel.Phrase;
import aleks.kuzko.datamodel.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created by Aleks on 26.04.2017.
 */

@RestController
@RequestMapping(value = "/rest")
public class SpringRestController {

    @Autowired
    PhraseDao phraseDao;

    @Autowired
    UserDao userDao;

    @RequestMapping(value = "/phrases", method = RequestMethod.GET)
    public List<Phrase> fetchAllPhrases(@RequestParam("user_id") long userId){
        return phraseDao.fetchAll(userId);
    }

    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public List<User> fetchAllUsers(){
        return userDao.fetchAll();
    }
}
