package main;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;

import javax.swing.JPanel;

import piece.Bishop;
import piece.King;
import piece.Knight;
import piece.Pawn;
import piece.Piece;
import piece.Queen;
import piece.Rook;

public class GamePanel extends JPanel implements Runnable {

	public static final int WIDTH = 1100;
	public static final int HEIGHT = 800;
	final int FPS = 60;
	Thread gameThread;
	Board board = new Board();
	Mouse mouse = new Mouse();
	
	// PIECES 
	public static ArrayList<Piece> pieces = new ArrayList<>();
	public static ArrayList<Piece> simPieces = new ArrayList<>();
	ArrayList<Piece> promoPieces = new ArrayList<>();
	Piece activeP, checkingP;
	public static Piece castlingP; 
	
	
	// COLOR
	public static final int WHITE = 0;
	public static final int BLACK = 1;
	int currentColor = WHITE;
	
	// BOOLEANS
	boolean canMove;
	boolean validSquare;
	boolean promotion;
	boolean gameover;
	boolean stalemate;
	
	
	public GamePanel() {
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setBackground(Color.black);
		addMouseMotionListener(mouse);
		addMouseListener(mouse);
		
		testIllegal();
		
		//setPieces();

		copyPieces(pieces, simPieces);
		
	}
	
	public void launchGame() {
		gameThread = new Thread(this);
		gameThread.start();
		
	}
	public void setPieces() {
		
		// White team
		pieces.add(new Pawn(WHITE,0,6));
		pieces.add(new Pawn(WHITE,1,6));
		pieces.add(new Pawn(WHITE,2,6));
		pieces.add(new Pawn(WHITE,3,6));
		pieces.add(new Pawn(WHITE,4,6));
		pieces.add(new Pawn(WHITE,5,6));
		pieces.add(new Pawn(WHITE,6,6));
		pieces.add(new Pawn(WHITE,7,6));
		pieces.add(new Knight(WHITE, 1, 7));
		pieces.add(new Knight(WHITE, 6, 7));
		pieces.add(new Rook(WHITE, 0, 7	));
		pieces.add(new Rook(WHITE, 7, 7));
		pieces.add(new Bishop(WHITE, 2, 7));
		pieces.add(new Bishop(WHITE, 5, 7));
		pieces.add(new Queen(WHITE, 3, 7));
		pieces.add(new King(WHITE, 4, 7));
		// Black team
		
		pieces.add(new Pawn(BLACK,0,1));
		pieces.add(new Pawn(BLACK,1,1));
		pieces.add(new Pawn(BLACK,2,1));
		pieces.add(new Pawn(BLACK,3,1));
		pieces.add(new Pawn(BLACK,4,1));
		pieces.add(new Pawn(BLACK,5,1));
		pieces.add(new Pawn(BLACK,6,1));
		pieces.add(new Pawn(BLACK,7,1));
		pieces.add(new Knight(BLACK, 1, 0));
		pieces.add(new Knight(BLACK, 6, 0));
		pieces.add(new Rook(BLACK, 0, 0));
		pieces.add(new Rook(BLACK, 7, 0));
		pieces.add(new Bishop(BLACK, 2, 0));
		pieces.add(new Bishop(BLACK, 5, 0));
		pieces.add(new Queen(BLACK, 3, 0));
		pieces.add(new King(BLACK, 4, 0));
		
	}
	
	public void testIllegal() {
		pieces.add(new Pawn(WHITE,7,4));
		pieces.add(new Rook(WHITE,0,7));
		pieces.add(new Rook(WHITE,7,2));
		pieces.add(new Queen(WHITE,1,7));
		pieces.add(new Knight(WHITE,5,5));
		pieces.add(new King(WHITE,7,7));
		pieces.add(new King(BLACK,0,0));
		pieces.add(new Pawn(BLACK,5,4));
		pieces.add(new Pawn(BLACK,7,3));
		pieces.add(new Bishop(BLACK,0,5));
		
	}
	private void copyPieces(ArrayList<Piece> source, ArrayList<Piece> target) {
		
		target.clear();
		for(int i = 0; i < source.size(); i++) {
			target.add(source.get(i));
		}
	}
	@Override
	public void run() {
		
		// GAME LOOP
		double drawInterval = 1000000000/FPS;
		double delta = 0;
		long lastTime = System.nanoTime();
		long currentTime;
		
		while(gameThread != null) {
			
			currentTime = System.nanoTime();
			
			delta += (currentTime - lastTime)/drawInterval;
			lastTime = currentTime;
			
			if(delta >= 1) {
				update();
				repaint();
				delta--;
				
			}
		}
		
	}
	
	private void update() {
		if(promotion) {
			promoting();
		}
		else if(gameover == false && stalemate == false){
			// MOUSE BUTTON PRESSED
			if(mouse.pressed) {
				if (activeP == null) {
					// If the activeP is null, check if you can pick up a piece
					for(Piece piece : simPieces) {
						// If the mouse is on an ally piece, it can pick it up as the activeP
						if(piece.color == currentColor &&
								piece.col == mouse.x/Board.SQUARE_SIZE &&
								piece.row == mouse.y/Board.SQUARE_SIZE) {
							
							activeP = piece;
						}
					}
						
				}
				else {
					// If the player is holding a piece, it can simulate the possible moves *not confirmed yet*
					simulate();
					
				}
			}
			
			// MOUSE BUTTON RELEASED //
			if(mouse.pressed == false) {
				
				if(activeP != null) {
					
					if(validSquare) { // if move is legal
						
						// MOVE CONFIRMED
						
						// Update the piece list in case a piece has been captured and removed during the simulation.
						copyPieces(simPieces, pieces);
						activeP.updatePosition(); // found in piece.java
						if(castlingP != null) {
							castlingP.updatePosition();
						}
						
						if(isKingInCheck() && isCheckmate()) {	
							gameover = true;
						}
						else if(isStalemate() && isKingInCheck() == false) {
							stalemate = true;
						}
						 else { // The game is still going on
							if(canPromote()) {
								promotion = true;
							}
							else {
								changePlayer();
							}						
						}
					}
					else {
						// The move is not valid so reset everything
						copyPieces(pieces, simPieces);
						activeP.resetPosition();
						activeP = null; // makes null because the player has released the piece
					}
				}
			}
		}
	}
	
	private void simulate() {
		
		canMove = false;
		validSquare = false;
		
		//Reset the piece list in every loop
		//This is basically for restoring the removed piece during the simulation
		copyPieces(pieces, simPieces);
		
		// Reset the castling piece's position
		if(castlingP != null) {
			castlingP.col = castlingP.preCol;
			castlingP.x = castlingP.getX(castlingP.col);
			castlingP = null;
		}
		
		// If a piece is being held, update its positions 
		// minus Board half square size so the mouse drags icon from middle and not top left corner.
		activeP.x = mouse.x - Board.HALF_SQUARE_SIZE;
		activeP.y = mouse.y - Board.HALF_SQUARE_SIZE;
		activeP.col = activeP.getCol(activeP.x);
		activeP.row = activeP.getRow(activeP.y);
		
		// Check if the piece is hovering over a reachable square
		if(activeP.canMove(activeP.col,  activeP.row)) {
			
			canMove = true;
			
			// IF hitting a piece, remove it from the list
			if(activeP.hittingP != null) {
				simPieces.remove(activeP.hittingP.getIndex());
			}
			
			checkCastling();
			
			if(isIllegal(activeP) == false && opponentCanCaptureKing() == false) {
				validSquare = true;
			}
		}
	}
	// For testing illegal movement of the king.
	private	boolean isIllegal(Piece king) {
		if(king.type == Type.KING) {
			for(Piece piece : simPieces) {
				  if(piece != king && piece.color != king.color && piece.canMove(king.col, king.row)) {
					   return true;
				  }
			}	
		}
		return false;
	}
	
	
	private boolean opponentCanCaptureKing() {
		
		Piece king  = getKing(false);
		
		for(Piece piece : simPieces) {
			if(piece.color != king.color && piece.canMove(king.col, king.row)) {
				return true;
			}
		}
		return false;
			
	}
	
	// copy of the function above, but checks if the opponent's king can move
	private boolean opponentCanCaptureKing2() {
		
		Piece king = getKing(true);
		
		for(Piece piece : simPieces) {
			if(king != null && piece.color != king.color && piece.canMove(king.col, king.row)) {
				return true;
			}
		}
		return false;
			
	}
	private boolean isKingInCheck() {
		
		Piece king = getKing(true);
		
		if(activeP.canMove(king.col, king.row)) {
			checkingP = activeP;
			return true;
			
		}
		else {
			checkingP = null;
		}
		
		return false;
	}
	
	private Piece getKing(boolean opponent) {
		
		Piece king = null;
		
		for(Piece piece : simPieces) {
			if(opponent) {
				if(piece.type == Type.KING && piece.color != currentColor) {
					king = piece;
				}
			}
			else {
				if(piece.type == Type.KING && piece.color == currentColor) {
					king = piece;
				}
			}
		}
		return king;
	}
	private boolean	isCheckmate() {
		
		Piece king = getKing(true);
		
		if(kingCanMove(king)) {
			return false;
		}
		else {
			// But you still have a chance!
			// Check if you can block the attack with your piece
			
			// Check the position of the checking piece and the king in check
			int colDiff = Math.abs(checkingP.col - king.col);
			int rowDiff = Math.abs(checkingP.row - king.row);
			
			if (colDiff == 0) {
				//The checking piece is attacking vertically
				if(checkingP.row < king.row) {
					// The checking piece is above the king
					for(int row = checkingP.row; row < king.row; row++) {
							for(Piece piece : simPieces ) {
								if(piece != king && piece.color != currentColor && piece.canMove(checkingP.col,row )) {
									return false;
								}
							}
						}
					}
				if(checkingP.row > king.row) {
					// The checking piece is below the king
						for(int row = checkingP.row; row > king.row; row--) {
								for(Piece piece : simPieces ) {
									if(piece != king && piece.color != currentColor && piece.canMove(checkingP.col,row )) {
										return false;
									}
								}
							}
						}
			}
			else if(rowDiff == 0) {
				// The checking piece is attacking horizontally
				
				if(checkingP.col < king.col) {
					// The checking	piece is to the left
						for(int col = checkingP.col; col < king.col; col++) {
								for(Piece piece : simPieces ) {
									if(piece != king && piece.color != currentColor && piece.canMove(col,checkingP.row )) {
										return false;
									}
								}
							}
						}
				if(checkingP.col > king.col) {
					// 	The checking piece is to the right
							for(int col = checkingP.col; col > king.col; col--) {
									for(Piece piece : simPieces ) {
										if(piece != king && piece.color != currentColor && piece.canMove(col,checkingP.row )) {
											return false;
										}
									}
								}
							}
				}
				else if(colDiff == rowDiff) {
					// The checking piece is attacking diagonally
					if(checkingP.row < king.row) {
						// The checking piece is above the king
						if(checkingP.col < king.col) {
							// The checking piece is in the upper left
							for(int col = checkingP.col, row = checkingP.row; col < king.col; col++, row++) {
								for(Piece piece : simPieces) {
									if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
										return false;
									}
								}
							}
						}
						if(checkingP.col > king.col ) {
							// The checking piece is in the upper right
							for(int col = checkingP.col, row = checkingP.row; col > king.col; col--, row++) {
								for(Piece piece : simPieces) {
									if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
										return false;
									}
								}
							}
						}	
					}
					if(checkingP.row > king.row) {
						// The checking piece is below the king 
						if(checkingP.col < king.col ) {
							// The checking piece is in the lower left
							for(int col = checkingP.col, row = checkingP.row; col < king.col; col++, row--) {
								for(Piece piece : simPieces) {
									if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
										return false;
									}
								}
							}
						}
						if(checkingP.col > king.col) {
							// The checking piece is in the lower right
							for(int col = checkingP.col, row = checkingP.row; col > king.col; col--, row--) {
								for(Piece piece : simPieces) {
									if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
										return false;
									}
								}
							}
						}
					}
			} 
			else {
				// The checking piece is Knight
			}
		}
		
		return true;
		
	}
	private boolean kingCanMove(Piece piece) {
		// checks if king can move 1 square around itself
		if(isValidMove(piece, -1, -1)) {return true;}
		if(isValidMove(piece, 0, -1)) {return true;}
		if(isValidMove(piece, 1, -1)) {return true;}
		if(isValidMove(piece, -1, 0)) {return true;}
		if(isValidMove(piece, 1, 0)) {return true;}
		if(isValidMove(piece, -1, 1)) {return true;}
		if(isValidMove(piece, 0, 1)) {return true;}
		if(isValidMove(piece, 1, 1)) {return true;}
		return false;
	}
	
	private boolean isValidMove(Piece king, int colPlus, int rowPlus) {
		
		boolean isValidMove = false;
		
		//Update the king's position for a second
		
		king.col += colPlus;
		king.row += rowPlus;
		
		if(king.canMove(king.col, king.row)) {
			
			// if piece is going to hit a piece remove that piece
			if (king.hittingP != null) {
			    simPieces.remove(king.hittingP.getIndex());
			}
			
			if (king.type == Type.KING){
				if(isIllegal(king) == false) {
					isValidMove = true;
				}
			} 			
		}
		
		// Reset the king's position and restore the removed piece
		king.resetPosition();
		copyPieces(pieces, simPieces);
		
		return isValidMove;
	}
	
	private boolean isValidMoveP(Piece piece, int colPlus, int rowPlus) {
		boolean isValidMove = false;
		
		//update the piece's position for a second
		
		piece.col += colPlus;
		piece.row += rowPlus;
		
		if (piece.canMove(piece.col, piece.row)) {
			if (piece.hittingP != null ) {
				simPieces.remove(piece.hittingP.getIndex());
				simPieces.remove(piece.getIndex());
			}
			if (opponentCanCaptureKing2() == false) {
				isValidMove = true;
			}
		}
		
		piece.resetPosition();
		copyPieces(pieces, simPieces);
		return isValidMove;
	}
	
	// checks if piece can move , use isValidMove
	// edited for all pieces, not just king.
	private boolean pieceCanMove(Piece piece) {
		
		if(isValidMove(piece, -1, -1)) {return true;}
		if(isValidMove(piece, 0, -1)) {return true;}
		if(isValidMove(piece, 1, -1)) {return true;}
		if(isValidMove(piece, -1, 0)) {return true;}
		if(isValidMove(piece, 1, 0)) {return true;}
		if(isValidMove(piece, -1, 1)) {return true;}
		if(isValidMove(piece, 0, 1)) {return true;}
		if(isValidMove(piece, 1, 1)) {return true;}
		
		// pawn
		if(isValidMoveP(piece, -1, -1)) {return true;}
		if(isValidMoveP(piece, -1, 1)) {return true;}
		if(isValidMoveP(piece, 0, -1)) {return true;}
		if(isValidMoveP(piece, 0, 1)) {return true;}
		if(isValidMoveP(piece, 1, -1)) {return true;}
		if(isValidMoveP(piece, 1, 1)) {return true;}

		// knight
		if(isValidMoveP(piece, 2, 1)) {return true;}
		if(isValidMoveP(piece, 2, -1)) {return true;}
		if(isValidMoveP(piece, -2, 1)) {return true;}
		if(isValidMoveP(piece, -2, -1)) {return true;}
		if(isValidMoveP(piece, 1, 2)) {return true;}
		if(isValidMoveP(piece, 1, -2)) {return true;}
		if(isValidMoveP(piece, -1, 2)) {return true;}
		if(isValidMoveP(piece, -1, -2)) {return true;}
		
		// bishop, rook, and queen
		for(int i = 0; i < 8; i++) {
			for(int j = 0; j < 8; j++) {
				if (i == 0) {
					if(isValidMoveP(piece, i, j)) {return true;}
				}
				if (j == 0) {
					if(isValidMoveP(piece, i, j)) {return true;}
				}
				if (Math.abs(i) == Math.abs(j)) {
					if(isValidMoveP(piece, i, j)) {return true;}
				}
			}
		}
		return false;
	}

	private boolean isStalemate() {
		/* A piece is in stalemate if 
		 * BOTH the king cannot move (while not in check) and pieces on the board cannot move
		 * i.e. blocked pawns or pieces pinned.
		 */
		
		int count = 0;
		// Count the number of pieces 
		/*This counts the number of pieces of the opponents* color (black)
		 * This will be used for identifying if only black's king is left
		 */
		// System.out.println(currentColor);
		for(Piece piece : simPieces) {
			if(piece.color != currentColor) {
				count++;
			}
		}
		
		// If only one piece (the king) is left
		if(count == 1) {
			if(kingCanMove(getKing(true)) == false) {
				return true;
			}
			
		// cannot checkmate when only king is left
		if(simPieces.size() == 2) {
			return true;
			}
		}
		
		//cannot checkmate with knight and king only
		if(simPieces.size() == 3) {
			for (Piece piece1 :simPieces) {
				if (piece1.type == Type.KNIGHT) {return true;}
			}
		}
		
		//checks every piece to see if they can move
		ArrayList<Piece> tempSimPieces = new ArrayList<>(simPieces);
		for(Piece piece : tempSimPieces) {
			if(piece.color != currentColor) {
				if (pieceCanMove(piece) == true) {
					return false;
				}
			}
		}
		return true;
	}
	
	

	private void checkCastling() {
		
		if(castlingP != null) {
		if(castlingP.col == 0) {
			castlingP.col += 3;
		}
		else if(castlingP.col == 7) {
			castlingP.col -= 2;
		}
		castlingP.x = castlingP.getX(castlingP.col);
		
		}
	}
	private void changePlayer() {
		if(currentColor == WHITE) {
			currentColor = BLACK;
			// Reset black's two stepped status
			for(Piece piece : pieces) {
				if(piece.color == BLACK) {
					piece.twoStepped = false;
					
				}
			}
		}
		else {
			currentColor = WHITE;
			// Reset white's two stepped status
			for(Piece piece	 : pieces) {
				if(piece.color == WHITE) {
					piece.twoStepped = false;
				}
 			}
		}
		activeP = null;
	}
	private boolean canPromote() {
		
		if(activeP.type == Type.PAWN) {
			if(currentColor ==  WHITE && activeP.row == 0 || currentColor == BLACK && activeP.row == 7) {
						promoPieces.clear();
						promoPieces.add(new Rook(currentColor,9,2));
						promoPieces.add(new Knight(currentColor,9,3));
						promoPieces.add(new Bishop(currentColor,9,4));
						promoPieces.add(new Queen(currentColor,9,5));
						return true;
				}
			}
			return false;	
	}
	private void promoting() {
		if(mouse.pressed) {
			for(Piece piece : promoPieces) {
				if(piece.col == mouse.x/Board.SQUARE_SIZE && piece.row == mouse.y/Board.SQUARE_SIZE) {
					switch(piece.type) {
					case ROOK: simPieces.add(new Rook(currentColor, activeP.col, activeP.row)); break;
					case KNIGHT: simPieces.add(new Knight(currentColor, activeP.col, activeP.row)); break;
					case BISHOP: simPieces.add(new Bishop(currentColor, activeP.col, activeP.row)); break;
					case QUEEN: simPieces.add(new Queen(currentColor, activeP.col, activeP.row)); break;
					default: break;
					}
					simPieces.remove(activeP.getIndex());
					copyPieces(simPieces, pieces);
					activeP = null;
					promotion = false;
					changePlayer();
				}
			}
		}
		
	}
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		Graphics2D g2 = (Graphics2D)g;
		
		//BOARD
		board.draw(g2);
		
		// PIECES
		for(Piece p : simPieces) {
			p.draw(g2);
			
		}
		
		// is the active piece is not null (active piece is held), change tile color to white and opacity to 0.7
		if(activeP != null) {
			if (canMove) {
				if(isIllegal(activeP) || opponentCanCaptureKing()) { // if it is illegal turn or the opponent can capture the king, the active piece (other piece trying to move during check 
					g2.setColor(Color.gray); // The tile will become gray indicating you can't move there/illegal move
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
					g2.fillRect(activeP.col*Board.SQUARE_SIZE, activeP.row*Board.SQUARE_SIZE,
							Board.SQUARE_SIZE, Board.SQUARE_SIZE);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
				}
				else {
					g2.setColor(Color.white);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
					g2.fillRect(activeP.col*Board.SQUARE_SIZE, activeP.row*Board.SQUARE_SIZE,
							Board.SQUARE_SIZE, Board.SQUARE_SIZE);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));	
				}
			}
	
			// Draw the active piece in the end so it won't be hidden by the board or the colored square
			activeP.draw(g2);
			
		}
		// STATUS MESSAGES
		g2.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setFont(new Font("Book Antiqua", Font.PLAIN, 40));
		g2.setColor(Color.white);
		
		if(promotion) {
			g2.drawString("Promote to:", 840, 150);
			for(Piece piece : promoPieces) {
				g2.drawImage(piece.image, piece.getX(piece.col), piece.getY(piece.row), 
						Board.SQUARE_SIZE, Board.SQUARE_SIZE, null);
			}
		}
		else {
			if(currentColor == WHITE) {
				g2.drawString("White's turn", 840, 550);
				if(checkingP != null && checkingP.color == BLACK) {
					g2.setColor(Color.red);
					g2.drawString("The King", 840, 650);
					g2.drawString("is in check!", 840, 700);
					
				}
			}
			else {
				g2.drawString("Black's Turn", 840, 250);
					if(checkingP != null && checkingP.color == WHITE) {
						g2.setColor(Color.red);
						g2.drawString("The King", 840, 100);
						g2.drawString("is in check!", 840, 150);
						
					}
				}
			}
		
		if(gameover) {
			String s = "";
			if(currentColor == WHITE) {
				s = "White Wins!";
			}
			else {
				s = "Black Wins!";
			}
			g2.setFont(new Font("Arial,", Font.PLAIN, 90));
			g2.setColor(Color.green);
			g2.drawString(s, 170, 420);
			}
		if (stalemate) {
			g2.setFont(new Font("Arial,", Font.PLAIN, 90));
			g2.setColor(Color.lightGray);
			g2.drawString("stalemate", 170, 420);
			
			}
		}
	}
