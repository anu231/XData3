package parsing;

import java.util.List;
import java.util.Vector;

import org.apache.derby.iapi.error.StandardException;
import org.apache.derby.impl.sql.compile.*;

public class JoinTreeNode {

	public static final String joinNode = "JOIN";
	public static final String relation = "RELATION";

	private String nodeType;
	private JoinTreeNode left, right;
	private Vector<Node> joinPred;
	private boolean isInnerJoin;
	private String relName;
	private String nodeAlias;
	private int maxOc;
	private int oc;	//Output Cardinality. 
	//Set the output cardinality of its children according to the properties of the ColRefs involved in the join Pred
	public boolean isUsingClause;
	public List<net.sf.jsqlparser.schema.Column> usingClause;
	public BinaryRelationalOperatorNode onNode;
	public JoinTreeNode(){
		//Constructor. Initialise things here.
		oc = 0;
		maxOc = 0;
		nodeAlias = "";
		joinPred = new Vector<Node>();
	}
	
	public String getNodeAlias() {
		return nodeAlias;
	}

	public void setNodeAlias(String nodeAlias) {
		this.nodeAlias = nodeAlias;
	}

	public String getNodeType() {
		return nodeType;
	}

	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}

	public JoinTreeNode getLeft() {
		return left;
	}

	public void setLeft(JoinTreeNode left) {
		this.left = left;
	}

	public JoinTreeNode getRight() {
		return right;
	}

	public void setRight(JoinTreeNode right) {
		this.right = right;
	}

	public Vector<Node> getJoinPred() {
		return joinPred;
	}

	public void addJoinPred(Node joinPred) {
		this.joinPred.add(joinPred);
	}

	public void setOnNode(ValueNode preds) {		
		if(preds instanceof BinaryRelationalOperatorNode) 
			onNode=(BinaryRelationalOperatorNode) preds;
	}
	
	public boolean isInnerJoin() {
		return isInnerJoin;
	}

	public void setInnerJoin(boolean isInnerJoin) {
		this.isInnerJoin = isInnerJoin;
	}

	public String getRelName() {
		return relName;
	}

	public void setRelName(String relName) {
		this.relName = relName;
	}

	public int getOc() {
		return oc;
	}

	public void setOc(int oc) {
		this.oc = oc;
	}
	
	public void whichIsFromLeft(Node cond){
		Node left = cond.getLeft();
		Node right = cond.getRight();
		Boolean leftInLeft = isPresent(this.getLeft(), left.getTableAlias());
		Boolean rightInRight = isPresent(this.getRight(), right.getTableAlias());	
	}
	
	public void setUsingClause(ResultColumnList columnList) {
		isUsingClause=true;
		usingClause=(columnList);
				
	}

	public Boolean isPresent(JoinTreeNode root, String alias){
		if(root.getNodeAlias().equalsIgnoreCase(alias)){
			return true;
		}
		else{
			if(isPresent(root.getLeft(), alias)) 
				return true;
			if (isPresent(root.getRight(), alias)) 
				return true;
		}
		return false;
	}
	
	public static void printTree(JoinTreeNode root){
		if(root.getNodeType().equalsIgnoreCase(JoinTreeNode.relation)){
			System.out.println(root.getRelName());
		}
		else{
			System.out.println(root.joinNode + root.getJoinPred().get(0).toString());
			printTree(root.getLeft());
			printTree(root.getRight());
		}
	}
	
	
	public String getJoinConditions(){
		
		String joinconds="(";
		if(isUsingClause){
			
			for (int i = 1; i <= usingClause.size(); i++) {				
				net.sf.jsqlparser.schema.Column column = usingClause.get(i-1);
				joinconds+=column.getColumnName()+",";
			}
			joinconds=joinconds.substring(0,joinconds.length()-1) +")";
		}else{
			
			String left=onNode.getLeftOperand().getTableName()+"."+onNode.getLeftOperand().getColumnName();
			String right=onNode.getRightOperand().getTableName()+"."+onNode.getRightOperand().getColumnName();
			joinconds="("+left+"="+right+")";
		}
		return joinconds;
	}
	public String getJoinClause(){
		
		if(isUsingClause){
			return (" USING "+getJoinConditions());			
		}
		return (" ON "+getJoinConditions());		
	}
	
	public String printNode(JoinTreeNode node){
		
		String out="";
		if(node.getNodeType().equalsIgnoreCase(JoinTreeNode.relation)){
			if(node.getNodeAlias().equals(""))
				return node.getRelName();
			else
				return node.getRelName() +" as "+node.getNodeAlias();
		}
		if(node.isInnerJoin){			
			if(!isUsingClause){				
				out = "("+ node.printNode(node.getLeft())+ " INNER JOIN "+node.printNode(node.getRight())+" ON "+node.getJoinConditions()+")";
			}				
			else{			
				out = "("+ node.printNode(node.getLeft())+ " INNER JOIN "+node.printNode(node.getRight())+" USING "+node.getJoinConditions()+")";
			}
				
		}
		else if(node.getNodeType().contains("LEFT")){
			
			if(!isUsingClause){				
				out = "("+ node.printNode(node.getLeft())+ " LEFT OUTER JOIN "+node.printNode(node.getRight())+" ON "+node.getJoinConditions()+")";
			}				
			else{				
				out = "("+ node.printNode(node.getLeft())+ " LEFT OUTER JOIN "+node.printNode(node.getRight())+" USING "+node.getJoinConditions()+")";
			}
				
		}		
		else if(node.getNodeType().contains("RIGHT")){
			if(!isUsingClause){
				out = "("+ node.printNode(node.getLeft())+ " RIGHT OUTER JOIN "+node.printNode(node.getRight())+" ON "+node.getJoinConditions()+")";
			}				
			else{
				out = "("+ node.printNode(node.getLeft())+ " RIGHT OUTER JOIN "+node.printNode(node.getRight())+" USING "+node.getJoinConditions()+")";
			}
				
		}
		
		return out;
	}
	
	public Vector<String> getUsingClauseColumns(JoinTreeNode node){
		Vector <String>col=new Vector<String>();
		if(!node.getNodeType().equalsIgnoreCase(JoinTreeNode.relation)){
			if(node.isUsingClause){
				//FIXME
				for(String st:node.usingClause.getColumnNames())
					col.add(st);
				col.addAll(getUsingClauseColumns(node.getLeft()));
				col.addAll(getUsingClauseColumns(node.getRight()));
				
			}
					
		}
		return col;
	}
	
	public Vector<String> getJoinTables(JoinTreeNode node){
		Vector <String>tab=new Vector<String>();
		if(node.getNodeType().equalsIgnoreCase(JoinTreeNode.relation)){				
				tab.add(node.getRelName());					
		}else{
			tab.addAll(getJoinTables(node.getLeft()));
			tab.addAll(getJoinTables(node.getRight()));			
			
		}
		return tab;		
	}	
	
}
