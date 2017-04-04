package datamodel;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Created by Aleks on 27.03.2017.
 */
public class TrainingLogQuestionLine {

    private final static String RIGHT_MESSAGE_COLOR = "green";
    private final static String WRONG_MESSAGE_COLOR = "#FF0000";
    private final static String NON_ANSWERED_MESSAGE_COLOR = "#BBBBB9";
    private final static String WRONG_MESSAGE = "wrong";
    private final static String RIGHT_MESSAGE = "right";

    public String getResultString() {
        return resultString;
    }

    private final String resultString;

    public TrainingLogQuestionLine(Question question){

        String timeAndRightWrongMessage = "[" + formatTime(question.getAskDate()) + " " + makeRightWrongMsg(question) + "] ";
        String phrase = question.getAskedPhrase().getNativeWord();
        String result;
        Phrase askedPhrase = question.getAskedPhrase();

        if (question.answered()){
            phrase += " - " + askedPhrase.getForeignWord();
            if(askedPhrase.getTranscription() != null && !askedPhrase.getTranscription().equals("")){
                phrase += " [" + askedPhrase.getTranscription() + "]";
            }
        }

        if(askedPhrase.isTrained()){
            phrase = makeStrong(applyColor(phrase, RIGHT_MESSAGE_COLOR));
        }

        result = timeAndRightWrongMessage + phrase;

        result = makeNewLine(result);

        resultString = result;
    }



    private String formatTime(ZonedDateTime givenTime){
        return givenTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private String makeStrong(String givenString){
        return "<strong>" + givenString + "</strong>";
    }

    private String applyColor(String givenString, String color){
        return "<font color=\"" + color + "\">" + givenString + "</font>";
    }

    private String makeNewLine(String givenString){
        return givenString + "</br>";
    }

    private String makeRightWrongMsg(Question givenQuestion){
        String result;
        if(!givenQuestion.answered()){
            result = applyColor(RIGHT_MESSAGE, NON_ANSWERED_MESSAGE_COLOR) + "/" + applyColor(WRONG_MESSAGE, NON_ANSWERED_MESSAGE_COLOR);
        }else if (givenQuestion.answerIsCorrect()){
            result = applyColor(RIGHT_MESSAGE, RIGHT_MESSAGE_COLOR) + "/" + applyColor(WRONG_MESSAGE, NON_ANSWERED_MESSAGE_COLOR);
        }else {
            result = applyColor(RIGHT_MESSAGE, NON_ANSWERED_MESSAGE_COLOR) + "/" + applyColor(WRONG_MESSAGE, WRONG_MESSAGE_COLOR);
        }
        return makeStrong(result);
    }

}
