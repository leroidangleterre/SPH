import java.awt.Graphics;

import javax.swing.JComponent;

public class ValuedScrollbar extends JComponent{

	private double min, max;
	private double value;

	public ValuedScrollbar(double min, double max, double value){
		this.min = Math.min(min, max);
		this.max = Math.max(min, max);
		if (this.max == this.min){
			this.max += 1;
		}
		this.value = Math.max(this.min, Math.min(this.max, value));
	}

	public ValuedScrollbar(double min, double max){
		this(min, max, 0);
	}

	public ValuedScrollbar(double value){
		this(0, 1, value);
	}

	public ValuedScrollbar(){
		this(0, 1, 0);
	}

	@Override
	public void paintComponent(Graphics g){

	}
}