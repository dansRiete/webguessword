package logic;

/**
 * Created by Aleks on 27.02.2016.
 * Contains methods for converting phrases into clues used in the learning process
 */
public class Hints {
    /**
     * Gets the phrase in English as a parameter. If a slash occurs in the phrase (for example: car \ my auto) returns
     * a hint *** \ ** ****, if the phrase does not contain a slash ("/") returns ""
     * @param engWord foreign phrase
     * @return a hint *** \ ** ****, if the phrase does not contain a slash ("/") returns ""
     */
    public String longHint(String engWord){
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
                }else if(temp=='’'){
                    newHintAr[i]='’';
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

    public String shortHint(String word){

        int numberOfVariants = 1;   // There is at least one
        int serialNumberOfHint = 0;
        StringBuilder finalHint = new StringBuilder("");
        boolean wasFirstSlash = false;

        for(char currentChar : word.toCharArray()){     //Count numbers of variants
            if(currentChar == '/' || currentChar == '\\'){
                numberOfVariants++;
            }
        }

        if(numberOfVariants > 1){
            finalHint.append('(');
            for(int i = 0; i < numberOfVariants; i++){
                finalHint.append(wasFirstSlash ? "/" : "").append(++serialNumberOfHint);
                wasFirstSlash = true;
            }
            finalHint.append(')');
        }

        return finalHint.toString();

    }
}
