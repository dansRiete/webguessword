package aleks.kuzko.controller;

import aleks.kuzko.dao.PhraseDao;
import aleks.kuzko.dao.UserDao;
import aleks.kuzko.datamodel.Phrase;
import aleks.kuzko.datamodel.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    public ResponseEntity<List<Phrase>> fetchAllPhrases(Authentication authentication){
        List<Phrase> retrievedPhrases = phraseDao.fetchAll(authentication.getName());
        return new ResponseEntity<>(retrievedPhrases, HttpStatus.OK);
    }

    @RequestMapping(value = "/phrases/{id}", method = RequestMethod.GET)
    public ResponseEntity<List<Phrase>> updatePhrase(Authentication authentication){
        List<Phrase> retrievedPhrases = phraseDao.fetchAll(authentication.getName());
        return new ResponseEntity<>(retrievedPhrases, HttpStatus.OK);
    }
}
