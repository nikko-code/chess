package piece;

import main.GamePanel;

public class King extends Piece{

	public King(int color, int col, int row) {
		super(color, col, row);
		
		if (color == GamePanel.WHITE) {
			image = getImage("/piece/w-king");
		}
		else {
			image = getImage("/piece/b-king");
			
		}
	}
	
	public boolean canMove(int targetCol, int targetRow) {
		
		if(isWithinBoard(targetCol, targetRow)) {
			
			// if the king moves in a legal way, it must be equal to a difference of 1 tile. (This calculation covers non diagonals and diagonals)
			if (Math.abs(targetCol - preCol) + Math.abs(targetRow - preRow) == 1 ||
					Math.abs(targetCol - preCol) * Math.abs(targetRow- preRow) == 1) {     // Going diagonally = col: row ration needs to be 1:1
				return true;
			}
		}
		
		return false;
	}

}