package controller;

import datamodel.Phrase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * Created by Aleks on 26.04.2017.
 */

@Controller
@RequestMapping(value = "/rest")
public class RestController {

    @Autowired
    RestDatabaseHelper restDatabaseHelper;

    @RequestMapping(value = "/phrases", method = RequestMethod.GET)
    public @ResponseBody List<Phrase> getAllPhrases(@RequestParam("userId") long userId){
        return restDatabaseHelper.fetchAllPhrases(userId);
    }
}
