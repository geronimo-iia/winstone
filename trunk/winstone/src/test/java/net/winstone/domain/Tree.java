package net.winstone.domain;

import java.util.ArrayList;
import java.util.List;

public class Tree {
    
    private String name;
    
    List<Tree> pList = new ArrayList<Tree>();
    
    protected Tree() {
        super();
    }

    public Tree(String name) {
        super();
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Tree addChild(Tree child) {
        pList.add(child);
        return child;
    }
    
    public Tree addChild(String name) {
        return addChild(new Tree(name));
    }
    
}
