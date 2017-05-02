package controller;

import beans.InterfaceBean;
import dao.DatabaseHelper;
import datamodel.Phrase;
import datamodel.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aleks on 26.04.2017.
 */

@Controller
public class PhraseController {
    public final static String GET_ALL_PHRASES = "/rest/phrases";

//    @Autowired
    public DatabaseHelper getDatabaseHelper() {
        return databaseHelper;
    }

//    @Autowired
    public void setDatabaseHelper(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }


    DatabaseHelper databaseHelper;

    @RequestMapping(value = PhraseController.GET_ALL_PHRASES, method = RequestMethod.GET)
    public @ResponseBody List<Phrase> getAllPhrases(){
        List<Phrase> allPhrases = new ArrayList<>();
        User user = new User("test", "Test User", "test_pass", "test_email");
        Phrase phrase1 = new Phrase(12, "word", "word", "tra", 30, ZonedDateTime.now(), "lab", ZonedDateTime.now(), 1,null, user);
        Phrase phrase2 = new Phrase(23, "word2", "word2", "tra2", 30, ZonedDateTime.now(), "lab", ZonedDateTime.now(), 1,null, user);
        allPhrases.add(phrase1);

        allPhrases.add(phrase2);
//        return allPhrases;
//        return interfaceBean.getDatabaseHelper().retrieveActivePhrases();
        return getDatabaseHelper().retrieveActivePhrases();
    }

}
