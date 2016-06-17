package logic;

/**
 * Created by Aleks on 18.06.2016.
 */
public class Id {
    int id;
    double prob;
    long index_start;
    long index_end;

    Id(int id, double prob, long index_start, long index_end){
        this.id = id;
        this.prob = prob;
        this.index_start = index_start;
        this.index_end = index_end;
    }
}
