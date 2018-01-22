package boofcv.applications;

import boofcv.gui.image.ImageZoomPanel;
import georegression.struct.point.Point2D_F64;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class SelectPointPanel extends ImageZoomPanel
	implements MouseListener, KeyListener
{
	// list of points selected by the user
	List<List<Point2D_F64>> pointSets = new ArrayList<>();
	List<Point2D_F64> activeSet = new ArrayList<>();

	Point2D_F64 selected = null;

	Line2D.Double line = new Line2D.Double();

	boolean showLines = true;

	int renderClosedAt = -1;

	public SelectPointPanel() {
		disableSaveOnClick();
		panel.addMouseListener(this);
		panel.addKeyListener(this);
	}

	@Override
	protected void paintInPanel(AffineTransform tran,Graphics2D g2 ) {

		g2.setStroke(new BasicStroke(2));
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setFont(g2.getFont().deriveFont(24f));
		int r = 6;

		Ellipse2D.Double ellipse = new Ellipse2D.Double();
		Rectangle2D.Double rect = new Rectangle2D.Double();

		synchronized (pointSets) {
			if( showLines ) {
				for (List<Point2D_F64> set : pointSets) {
					renderLines(g2, set, 0, 0x00FFA0);
				}
			}

			for (List<Point2D_F64> set : pointSets) {
				for (int i = 0; i < set.size(); i++) {
					Point2D_F64 p = set.get(i);

					double x = p.x * scale;
					double y = p.y * scale;

					ellipse.setFrame(x - r, y - r, 2 * r, 2 * r);
					rect.setRect(x - 0.5, y - 0.5, 1, 1);

					g2.setColor(Color.RED);
					g2.draw(ellipse);
					g2.draw(rect);
//			g2.drawOval(x - r, y - r, w,w);
//			g2.drawRect(x,y,1,1);
					g2.setColor(Color.MAGENTA);
					g2.drawString(i + "", (int) x + 2 * r, (int) y);
				}
			}
		}


		if( selected != null ) {
			double x = selected.x * scale;
			double y = selected.y * scale;

			g2.setColor(Color.BLUE);
			r = 8;

			ellipse.setFrame(x-r,y-r,2*r,2*r);
			g2.draw(ellipse);
		}
	}

	private void renderLines( Graphics2D g2 , List<Point2D_F64> corners , int start , int color ) {
		if( corners.size() <= 1 )
			return;
		g2.setColor(new Color(color));
		int N = corners.size()-start;
//		for (int i = 0,j = N-1; i < N; j=i,i++) {
//			Point2D_F64 a = corners.get(i+start);
//			Point2D_F64 b = corners.get(j+start);
//
//			renderLine(g2,a,b);
//		}
		for (int i = 1; i < N; i++) {
			int j = i - 1;
			Point2D_F64 a = corners.get(i+start);
			Point2D_F64 b = corners.get(j+start);

			renderLine(g2,a,b);
		}

		if( renderClosedAt == N) {
			Point2D_F64 a = corners.get(0);
			Point2D_F64 b = corners.get(corners.size()-1);

			renderLine(g2,a,b);
		}
	}

	private void renderLine( Graphics2D g2 , Point2D_F64 cornerA , Point2D_F64 cornerB ) {
		double xa = cornerA.x * scale, ya = cornerA.y * scale;
		double xb = cornerB.x * scale, yb = cornerB.y * scale;

		line.setLine(xa,ya,xb,yb);
		g2.draw(line);
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {
		Point2D_F64 p = pixelToPoint(e.getX(), e.getY());

		if( e.getButton() == MouseEvent.BUTTON3 ) {
			centerView(p.x, p.y);
		} else {
			synchronized (pointSets) {
				double tol = 12 / scale;

				boolean found = false;
				for (List<Point2D_F64> set : pointSets) {
					for (int i = 0; i < set.size(); i++) {
						Point2D_F64 s = set.get(i);
						if (s.distance(p) <= tol) {
							activeSet = set;
							selected = s;
							found = true;
							break;
						}
					}
				}
				if (!found) {
					selected = p;
					activeSet.add(p);
					if (activeSet.size() == 1) {
						pointSets.add(activeSet);
					}
				}
			}
		}
		repaint();
		panel.requestFocus();
	}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {
		setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
	}

	@Override
	public void mouseExited(MouseEvent e) {
		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	@Override
	public void keyPressed(KeyEvent e) {

//		System.out.println("Key event "+e.getKeyCode()+"  0x"+Integer.toHexString(e.getKeyCode()));

		Point2D_F64 p = selected;

		double delta = 1.0/scale;

		switch( e.getKeyCode() ) {
			case KeyEvent.VK_S:
				activeSet = new ArrayList<>();
				break;

			case KeyEvent.VK_T:
				splitAtSelected();
				break;

			case KeyEvent.VK_NUMPAD4:
			case KeyEvent.VK_KP_LEFT:
				if( p != null )
					p.x -= delta;
				break;

			case KeyEvent.VK_NUMPAD6:
			case KeyEvent.VK_KP_RIGHT:
				if( p != null )
					p.x += delta;
				break;

			case KeyEvent.VK_NUMPAD2:
			case KeyEvent.VK_KP_DOWN:
				if( p != null )
					p.y += delta;
				break;

			case KeyEvent.VK_NUMPAD8:
			case KeyEvent.VK_KP_UP:
				if( p != null )
					p.y -= delta;
				break;

			case KeyEvent.VK_DELETE:
			case KeyEvent.VK_BACK_SPACE:
				if( selected != null ) {
					for (List<Point2D_F64> set : pointSets) {
						if (set.remove(selected)) {
							break;
						}
					}
					selected = null;
				}
				break;

			case KeyEvent.VK_NUMPAD7:
				if( p != null ) {
					p.x -= delta;
					p.y -= delta;
				}
				break;

			case KeyEvent.VK_NUMPAD9:
				if( p != null ) {
					p.x += delta;
					p.y -= delta;
				}
				break;

			case KeyEvent.VK_NUMPAD1:
				if( p != null ) {
					p.x -= delta;
					p.y += delta;
				}
				break;

			case KeyEvent.VK_NUMPAD3:
				if( p != null ) {
					p.x += delta;
					p.y += delta;
				}
				break;
		}

		repaint();
	}

	@Override
	public void keyReleased(KeyEvent e) {

	}

	public List<List<Point2D_F64>> getSelectedPoints() {
		return pointSets;
	}

	private void splitAtSelected() {
		if( selected == null )
			return;

		synchronized (pointSets) {
			for (List<Point2D_F64> set : pointSets) {
				if (set.contains(selected)) {
					int index = set.indexOf(selected);
					List<Point2D_F64> split = new ArrayList<Point2D_F64>();
					for (int i = index; i <set.size(); i++) {
						split.add(set.get(i));
					}
					while( set.size() > index ) {
						set.remove(index);
					}
					pointSets.add(split);
					activeSet = split;
					return;
				}
			}
		}
		throw new RuntimeException("BUG");
	}

	public void addPointSet(List<Point2D_F64> points) {
		pointSets.add(points);
		activeSet = points;
		selected = null;
	}

	public void clearPoints() {
		pointSets = new ArrayList<List<Point2D_F64>>();
		activeSet = new ArrayList<Point2D_F64>();
		selected = null;
	}

	public void setSets(List<List<Point2D_F64>> sets) {
		this.pointSets = sets;
	}
}
