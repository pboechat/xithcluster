package br.edu.univercidade.cc.xithcluster.composition;

public class TopDownRightToLeftBufferReadOrder extends BufferReadOrder {
	
	private int width;
	
	private int height;
	
	private int[] indexes;
	
	public TopDownRightToLeftBufferReadOrder(int width, int height) {
		super(width, height);
		
		this.width = width;
		this.height = height;
		
		computePixelIndexes();
	}
	
	private void computePixelIndexes() {
		indexes = new int[bufferSize];
		
		int pixelIndex = -1;
		int i = 0;
		for (int row = 0; row < height; row++) {
			pixelIndex += width;
			for (int column = 0; column < width; column++) {
				indexes[i++] = pixelIndex - column;
			}
		}
	}
	
	@Override
	public int nextIndex() {
		return indexes[lastReadIndex++];
	}
	
}
