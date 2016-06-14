package logic;

/**
 * Created by Aleks on 27.02.2016.
 * Класс UtilClasses.Hints содержит методы для превращения фраз в подсказки использующиеся в процессе учебного процесса
 */
public class Hints {
    /**
     * Получает фразу на английском в качестве параметра. Если в фразе встречается слеш (например: car\my auto) возвращает
     * подсказку типа ***\** ****, если фраза не содержит слеш("/") возвращает ""
     * @param engWord фразa на английском
     * @return Если в фразе встречается слеш (например: car\my auto) возвращает
     * подсказку типа ***\** ****, если фраза не содержит слеш("/") возвращает ""
     */
    public String getSlashHint(String engWord){
        if(engWord.contains("/")||engWord.contains("\\")||engWord.contains(" ")||engWord.contains("-")||
                engWord.contains("`")||engWord.contains("'")||engWord.contains(",")){
            char[] hintAr = engWord.toCharArray();
            char[] newHintAr = new char[hintAr.length+2];
            int i = 1;
                newHintAr[0]='(';
                newHintAr[newHintAr.length-1]=')';
            for(char temp : hintAr){
                if(temp==' '){
                    newHintAr[i]=' ';
                    i++;
                }else if(temp=='/'){
                    newHintAr[i]='/';
                    i++;
                }else if(temp=='-'){
                    newHintAr[i]='-';
                    i++;
                }else if(temp=='`'){
                    newHintAr[i]='`';
                    i++;
                }else if(temp=='\''){
                    newHintAr[i]='\'';
                    i++;
                }else if(temp=='\\'){
                    newHintAr[i]='\\';
                    i++;
                }else if(temp==','){
                    newHintAr[i]=',';
                    i++;
                }else{
                    newHintAr[i]='*';
                    i++;
                }
            }
            return new String(newHintAr);
    }else
        return "";
    }
}
