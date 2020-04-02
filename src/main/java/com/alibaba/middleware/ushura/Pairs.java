package com.alibaba.middleware.ushura;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Hanson on 15/12/25.
 */
public class Pairs {

    /**
     * String类型的权重数据生成工具，从一个Map转换成Chooser识别的数据结构
     * @param itemsWithWeight
     * @return
     */
    public static List<Pair<String>> formMap(Map<String,Double> itemsWithWeight){
        List<Pair<String>> pairs = new ArrayList<Pair<String>>();
        for(Map.Entry<String,Double> entry : itemsWithWeight.entrySet()){
            pairs.add(new Pair<String>(entry.getKey(),entry.getValue())) ;
        }
        return pairs;
    }
}
