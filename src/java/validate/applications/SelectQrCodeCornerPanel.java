package validate.applications;

import boofcv.gui.image.ImageZoomPanel;
import georegression.geometry.UtilLine2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_F64;
import validate.applications.HandSelectQRCodeApp.QRCorners;

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
public class SelectQrCodeCornerPanel extends ImageZoomPanel
	implements MouseListener, KeyListener
{
	// list of points selected by the user
	final List<QRCorners> markers = new ArrayList<>();
	QRCorners activeQR = null;

	Point2D_F64 selected = null;

	Ellipse2D.Double ellipse = new Ellipse2D.Double();
	Rectangle2D.Double rect = new Rectangle2D.Double();
	Line2D.Double line = new Line2D.Double();

	public SelectQrCodeCornerPanel() {
		disableSaveOnClick();
		panel.addMouseListener(this);
		panel.addKeyListener(this);
	}

	@Override
	protected void paintInPanel(AffineTransform tran,Graphics2D g2 ) {

		g2.setColor(Color.RED);
		g2.setStroke(new BasicStroke(2));
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		synchronized (markers) {
			for (QRCorners qr : markers) {
				renderQR(g2,qr);
			}
		}

		if( selected != null ) {
			double x = selected.x * scale;
			double y = selected.y * scale;

			g2.setColor(Color.BLUE);
			int r = 8;

			ellipse.setFrame(x-r,y-r,2*r,2*r);
			g2.draw(ellipse);
		}
	}

	private void renderQR( Graphics2D g2 , QRCorners qr ) {

		g2.setStroke(new BasicStroke(4));
		if( qr.corners.size() >= 4 ) {
			renderSquare(g2,qr.corners,0,0xFFA0A0);
		}
		if( qr.corners.size() >= 8 ) {
			renderSquare(g2,qr.corners,4,0xA0FFA0);
		}
		if( qr.corners.size() >= 12 ) {
			renderSquare(g2,qr.corners,8,0xA0A0FF);
			renderFullQR(g2,qr.corners);
		}

		int r = 6;
		g2.setStroke(new BasicStroke(1));
		g2.setColor(Color.RED);
		for (int i = 0; i < qr.corners.size(); i++) {
			Point2D_F64 p = qr.corners.get(i);

			double x = p.x * scale;
			double y = p.y * scale;

			ellipse.setFrame(x - r, y - r, 2 * r, 2 * r);
			rect.setRect(x - 0.5, y - 0.5, 1, 1);

			g2.draw(ellipse);
			g2.draw(rect);

			g2.drawString(i + "", (int) x + 2 * r, (int) y);
		}
	}

	private void renderSquare( Graphics2D g2 , List<Point2D_F64> corners , int start , int color ) {
		g2.setColor(new Color(color));
		for (int i = 0; i < 4; i++) {
			Point2D_F64 a = corners.get(i+ start);
			Point2D_F64 b = corners.get(((i+1)%4) + start);

			renderLine(g2,a,b);
		}
	}

	private void renderFullQR( Graphics2D g2 , List<Point2D_F64> corners ) {
		LineSegment2D_F64 segA = new LineSegment2D_F64(corners.get(0),corners.get(3));
		LineSegment2D_F64 segB = new LineSegment2D_F64(corners.get(10),corners.get(11));

		LineGeneral2D_F64 lineA = UtilLine2D_F64.convert(segA,(LineGeneral2D_F64)null);
		LineGeneral2D_F64 lineB = UtilLine2D_F64.convert(segB,(LineGeneral2D_F64)null);

		Point2D_F64 impliedCorner = new Point2D_F64();
		Intersection2D_F64.intersection(lineA,lineB,impliedCorner);

		renderLineBorder(g2,corners.get(3),impliedCorner,Color.BLUE);
		renderLineBorder(g2,corners.get(7),impliedCorner,Color.BLUE);
		renderLineBorder(g2,corners.get(11),impliedCorner,Color.BLUE);


		segA = new LineSegment2D_F64(corners.get(1),corners.get(2));
		segB = new LineSegment2D_F64(corners.get(9),corners.get(8));

		lineA = UtilLine2D_F64.convert(segA,(LineGeneral2D_F64)null);
		lineB = UtilLine2D_F64.convert(segB,(LineGeneral2D_F64)null);

		impliedCorner = new Point2D_F64();
		Intersection2D_F64.intersection(lineA,lineB,impliedCorner);

		renderLineBorder(g2,corners.get(2),impliedCorner,Color.MAGENTA);
		renderLineBorder(g2,corners.get(8),impliedCorner,Color.MAGENTA);
	}

	private void renderLine( Graphics2D g2 , Point2D_F64 cornerA , Point2D_F64 cornerB ) {
		double xa = cornerA.x * scale, ya = cornerA.y * scale;
		double xb = cornerB.x * scale, yb = cornerB.y * scale;

		line.setLine(xa,ya,xb,yb);
		g2.draw(line);
	}

	private void renderLineBorder( Graphics2D g2 , Point2D_F64 cornerA , Point2D_F64 cornerB, Color color ) {
		g2.setStroke(new BasicStroke(6));
		g2.setColor(color);
		double xa = cornerA.x * scale, ya = cornerA.y * scale;
		double xb = cornerB.x * scale, yb = cornerB.y * scale;

		line.setLine(xa,ya,xb,yb);
		g2.draw(line);

		g2.setStroke(new BasicStroke(1));
		g2.setColor(Color.WHITE);
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
			synchronized (markers) {
				double tol = 12 / scale;

				boolean found = false;
				for (QRCorners set : markers) {
					for (int i = 0; i < set.corners.size(); i++) {
						Point2D_F64 s = set.corners.get(i);
						if (s.distance(p) <= tol) {
							activeQR = set;
							selected = s;
							found = true;
							break;
						}
					}
				}
				if (!found) {
					selected = p;
					if( activeQR == null || activeQR.corners.size() >= 12 ) {
						activeQR = new QRCorners();
						activeQR.corners.add(p);
						markers.add(activeQR);
					} else {
						activeQR.corners.add(p);
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
		if( selected == null )
			return;

//		System.out.println("Key event "+e.getKeyCode()+"  0x"+Integer.toHexString(e.getKeyCode()));

		Point2D_F64 p = selected;

		double delta = 0.05;

		switch( e.getKeyCode() ) {
			case KeyEvent.VK_S:
				activeQR = null;
				break;

			case KeyEvent.VK_T:
//				splitAtSelected();
				break;

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
				if( activeQR != null ) {
					activeQR.corners.remove(selected);
				}
				selected = null;
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

	public List<QRCorners> getSelectedPoints() {
		return markers;
	}


//	public void addPointSet(List<Point2D_F64> points) {
//		markers.add(points);
//		activeSet = points;
//		selected = null;
//	}

	public void clearPoints() {
		markers.clear();
		activeQR = null;
		selected = null;
	}

//	public void setSets(List<List<Point2D_F64>> sets) {
//		this.markers = sets;
//	}
}
