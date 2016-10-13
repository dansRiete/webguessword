package logic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by Aleks on 25.07.2015.
 */
public class IntelliFind {

    public boolean match(String firstWord, String secondWord, boolean exactMatch) {

        if(firstWord.equalsIgnoreCase(secondWord))
            return true;
        else if(!exactMatch){

            String shortedWord = String.valueOf(firstWord.charAt(0));
            String shortAnswer;
            int numOfSyllables;
            for (int i = 1, y = 0; i < firstWord.length(); i++) {
                // убирает двойные буквы из правильного ответа
                if (firstWord.toLowerCase().charAt(i) != shortedWord.charAt(y)){
                    shortedWord += firstWord.toLowerCase().charAt(i);
                    y++;
                }
            }
            if(secondWord.length()==0){
                shortAnswer=secondWord;
            }
            else {
                shortAnswer = String.valueOf(secondWord.charAt(0));
                for (int i = 1, y = 0; i < secondWord.length(); i++) {
                    // убирает двойные буквы из ответа
                    if (secondWord.toLowerCase().charAt(i) != shortAnswer.charAt(y)) {
                        shortAnswer += secondWord.toLowerCase().charAt(i);
                        y++;
                    }
                }
            }
            if (shortedWord.equalsIgnoreCase(secondWord)){
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
//                        System.out.println("PatternSyntaxException in "+arWord[i]+" - "+shortAnswer);
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
