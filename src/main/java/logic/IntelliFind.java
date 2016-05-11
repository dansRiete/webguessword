package logic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by Aleks on 25.07.2015.
 */
public class IntelliFind {
    public static boolean match(String word,String answer,boolean exactMatch) {

        if(word.equalsIgnoreCase(answer))
            return true;
        else if(!exactMatch){

            String shortedWord = String.valueOf(word.charAt(0));
            String shortAnswer;
            int numOfSyllables;
            for (int i = 1, y = 0; i < word.length(); i++) {
                // убирает двойные буквы из правильного ответа
                if (word.toLowerCase().charAt(i) != shortedWord.charAt(y)){
                    shortedWord += word.toLowerCase().charAt(i);
                    y++;
                }
            }
            if(answer.length()==0){
                shortAnswer=answer;
            }
            else {
                shortAnswer = String.valueOf(answer.charAt(0));
                for (int i = 1, y = 0; i < answer.length(); i++) {
                    // убирает двойные буквы из ответа
                    if (answer.toLowerCase().charAt(i) != shortAnswer.charAt(y)) {
                        shortAnswer += answer.toLowerCase().charAt(i);
                        y++;
                    }
                }
            }
            if (shortedWord.equalsIgnoreCase(answer)){
    //            System.out.println("Сработал Интелифайнд");   //Сигнализирует о том что Интелифайнд вернул true
                return true;
            }
            else if (shortedWord.length() < 5)
                return false;
            else {
                //разбиваем shortWord на слоги
                numOfSyllables = shortedWord.length() / 2; //вычисляем количество слогов по3
                if (shortedWord.length() % 2 > 0)
                    numOfSyllables = numOfSyllables + 1;
                //System.out.println("Количество слогов в shortWord=" + b);

                String arWord[] = new String[numOfSyllables];
                for (int i = 0; i < numOfSyllables; i++) {
                    //Заполняем массив arWord[] слогами
                    arWord[i] = shortedWord.substring(i*2, (i*2+2 > shortedWord.length()) ? shortedWord.length() : i*2+2);
                }
                float matchedSyllables = 0;
                for (int i = 0; i < arWord.length; i++){
                    //поиск совпадений по слогам
                    try{
                        Pattern pat = Pattern.compile(arWord[i]);
                        Matcher match = pat.matcher(shortAnswer);
                        if (match.find())
                            matchedSyllables++;
                    }
                    catch (PatternSyntaxException e){
                        System.out.println("PatternSyntaxException in "+arWord[i]+" - "+shortAnswer);
                    }
                }
                float f=matchedSyllables/(float)numOfSyllables;
    //            if(f>0.7)
    //                System.out.println("Сработал Интелифайнд");   //Сигнализирует о том что Интелифайнд вернул true
                return f>0.7;

            }
        }else
            return false;

    }
}
