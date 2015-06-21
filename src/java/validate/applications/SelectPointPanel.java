package validate.applications;

import georegression.struct.point.Point2D_F64;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
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
	List<Point2D_F64> points = new ArrayList<Point2D_F64>();
	int selected = -1;

	public SelectPointPanel() {
		panel.addMouseListener(this);
		panel.addKeyListener(this);
	}

	@Override
	protected void paintInPanel(AffineTransform tran,Graphics2D g2 ) {

		g2.setColor(Color.RED);
		g2.setStroke(new BasicStroke(2));
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int r = 6;

		Ellipse2D.Double ellipse = new Ellipse2D.Double();
		Rectangle2D.Double rect = new Rectangle2D.Double();

		for (int i = 0; i < points.size(); i++) {
			Point2D_F64 p = points.get(i);

			double x = p.x * scale;
			double y = p.y * scale;

			ellipse.setFrame(x-r,y-r,2*r,2*r);
			rect.setRect(x-0.5,y-0.5,1,1);

			g2.draw(ellipse);
			g2.draw(rect);
//			g2.drawOval(x - r, y - r, w,w);
//			g2.drawRect(x,y,1,1);
			g2.drawString(i+"",(int)x+2*r,(int)y);
		}

		if( selected >= 0 ) {
			Point2D_F64 p = points.get(selected);
			double x = p.x * scale;
			double y = p.y * scale;

			g2.setColor(Color.BLUE);
			r = 8;

			ellipse.setFrame(x-r,y-r,2*r,2*r);
			g2.draw(ellipse);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {
		Point2D_F64 p = pixelToPoint(e.getX(), e.getY());

		if( e.getButton() == MouseEvent.BUTTON3 ) {
			centerView(p.x, p.y);
		} else {
			double tol = 12 / scale;

			boolean found = false;
			for (int i = 0; i < points.size(); i++) {
				Point2D_F64 s = points.get(i);
				if (s.distance(p) <= tol) {
					selected = i;
					found = true;
					break;
				}
			}
			if (!found) {
				selected = points.size();
				points.add(p);
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
		if( selected < 0 )
			return;

//		System.out.println("Key event "+e.getKeyCode()+"  0x"+Integer.toHexString(e.getKeyCode()));

		Point2D_F64 p = points.get(selected);

		double delta = 0.05;

		switch( e.getKeyCode() ) {
			case KeyEvent.VK_NUMPAD4:
			case KeyEvent.VK_KP_LEFT:
				p.x -= delta;
				break;

			case KeyEvent.VK_NUMPAD6:
			case KeyEvent.VK_KP_RIGHT:
				p.x += delta;
				break;

			case KeyEvent.VK_NUMPAD2:
			case KeyEvent.VK_KP_DOWN:
				p.y += delta;
				break;

			case KeyEvent.VK_NUMPAD8:
			case KeyEvent.VK_KP_UP:
				p.y -= delta;
				break;

			case KeyEvent.VK_DELETE:
			case KeyEvent.VK_BACK_SPACE:
				points.remove(selected);
				selected = -1;
				break;

			case KeyEvent.VK_NUMPAD7:
				p.x -= delta;
				p.y -= delta;
				break;

			case KeyEvent.VK_NUMPAD9:
				p.x += delta;
				p.y -= delta;
				break;

			case KeyEvent.VK_NUMPAD1:
				p.x -= delta;
				p.y += delta;
				break;

			case KeyEvent.VK_NUMPAD3:
				p.x += delta;
				p.y += delta;
				break;
		}

		repaint();
	}

	@Override
	public void keyReleased(KeyEvent e) {

	}

	public List<Point2D_F64> getSelectedPoints() {
		return points;
	}

	public void setPoints(List<Point2D_F64> points) {
		this.points = points;
		selected = -1;
	}

}
