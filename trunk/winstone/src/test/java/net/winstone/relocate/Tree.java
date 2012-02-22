package net.winstone.relocate;

import java.util.ArrayList;
import java.util.List;

public class Tree {

	private String name;

	List<Tree> pList = new ArrayList<Tree>();

	protected Tree() {
		super();
	}

	public Tree(final String name) {
		super();
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Tree addChild(final Tree child) {
		pList.add(child);
		return child;
	}

	public Tree addChild(final String name) {
		return addChild(new Tree(name));
	}

}
