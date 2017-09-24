package validate.applications;

import boofcv.gui.image.ImageZoomPanel;
import georegression.geometry.UtilEllipse_F64;
import georegression.metric.Distance2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.EllipseRotated_F64;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
// TODO adjust axis
// TODO rotate
public class SelectEllipsePanel extends ImageZoomPanel
	implements MouseListener, KeyListener, MouseMotionListener
{
	final List<EllipseRotated_F64> list = new ArrayList<EllipseRotated_F64>();

	EllipseRotated_F64 selected = null;
	Point2D_F64 seedPoint = new Point2D_F64();
	double prevAngle;

	Mode mode = Mode.IDLE;

	double tolerancePixels = 20;
	Color background = new Color(0,0,0,125);

	// if true the circles are connected an a line will be down to show their relationship
	boolean connectedCircles;

	Line2D.Double line = new Line2D.Double();

	public SelectEllipsePanel(boolean connectedCircles) {
		this.connectedCircles = connectedCircles;
		panel.addMouseListener(this);
		panel.addKeyListener(this);
		panel.addMouseMotionListener(this);
	}

	@Override
	protected void paintInPanel(AffineTransform tran,Graphics2D g2 ) {

		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		synchronized (list) {
			if( connectedCircles ) {
				g2.setColor(new Color(100, 100, 200));
				g2.setStroke(new BasicStroke(1));
				for (int i = 1; i < list.size(); i++) {
					EllipseRotated_F64 a = list.get(i - 1);
					EllipseRotated_F64 b = list.get(i);

					line.x1 = (float) (scale * a.center.x);
					line.y1 = (float) (scale * a.center.y);
					line.x2 = (float) (scale * b.center.x);
					line.y2 = (float) (scale * b.center.y);

					g2.draw(line);
				}
			}

			g2.setStroke(new BasicStroke(2));
			g2.setColor(Color.RED);
			for (int i = 0; i < list.size(); i++) {
				EllipseRotated_F64 ellipse = list.get(i);

				renderEllipse(g2, ellipse);

				float x = (float)(scale*(ellipse.center.x));
				float y = (float)(scale*((ellipse.center.y - ellipse.a) - 5));
				g2.setColor(background);
				g2.fillRect((int)(x-2),(int)(y-12),20,16);

				g2.setColor(Color.PINK);
				g2.drawString(String.format("%d",i),x,y);
			}
		}

	}

	private void renderEllipse(Graphics2D g2, EllipseRotated_F64 ellipse) {
		AffineTransform rotate = AffineTransform.getRotateInstance(ellipse.phi);
		AffineTransform translate = AffineTransform.getTranslateInstance(scale*ellipse.center.x,scale*ellipse.center.y);

		double w = scale*ellipse.a*2;
		double h = scale*ellipse.b*2;

		Shape shape = rotate.createTransformedShape(new Ellipse2D.Double(-w/2,-h/2,w,h));
		shape = translate.createTransformedShape(shape);

		g2.setColor(Color.RED);
		g2.draw(shape);

		// Draw the center
		shape = rotate.createTransformedShape(new Ellipse2D.Double(-3,-3,7,7));
		shape = translate.createTransformedShape(shape);;
		g2.draw(shape);

		// draw major axis
		shape = rotate.createTransformedShape(new Line2D.Double(-w/2,0,w/2,0));
		shape = translate.createTransformedShape(shape);
		g2.setColor(Color.BLUE);
		g2.draw(shape);

		// Draw axis end points
		shape = rotate.createTransformedShape(new Ellipse2D.Double(-w/2.0-2,-2,4.0,4.0));
		shape = translate.createTransformedShape(shape);
		g2.draw(shape);

		shape = rotate.createTransformedShape(new Ellipse2D.Double(w/2.0-2,-2,4.0,4.0));
		shape = translate.createTransformedShape(shape);
		g2.draw(shape);

		g2.setColor(Color.GREEN);
		shape = rotate.createTransformedShape(new Ellipse2D.Double(-2,-h/2.0-2,4,4));
		shape = translate.createTransformedShape(shape);
		g2.draw(shape);

		shape = rotate.createTransformedShape(new Ellipse2D.Double(-2,h/2.0-2,4,4));
		shape = translate.createTransformedShape(shape);
		g2.draw(shape);
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {
		Point2D_F64 p = pixelToPoint(e.getX(), e.getY());

		if( e.getButton() == MouseEvent.BUTTON3 ) {
			centerView(p.x, p.y);
		} else {
			boolean handled = false;
			if( selected != null ) {
				if( checkEnterAdjustAxis(p) ) {
					handled = true;
				}
			}
			if( ! handled ) {
				selected = findSelected(p);
				if (selected == null) {
					mode = Mode.DRAW_CIRCLE;
					selected = new EllipseRotated_F64(p.x, p.y, 0, 0, 0);
					seedPoint.set(p);
					synchronized (list) {
						list.add(selected);
					}
				} else {
					if (checkEnterAdjustAxis(p)) {
					}
				}
			}

		}
		repaint();
		panel.requestFocus();
	}

	private EllipseRotated_F64 findSelected( Point2D_F64 p ) {
		synchronized (list) {
			EllipseRotated_F64 best = null;
			double bestD = Double.MAX_VALUE;
			for (int i = 0; i < list.size(); i++) {
				EllipseRotated_F64 ellipse = list.get(i);

				if( Intersection2D_F64.contains(ellipse,p.x,p.y) ) {
					if( p.distance(ellipse.center) < bestD ) {
						bestD = p.distance(ellipse.center);
						best = ellipse;
					}
				}
			}
			return best;
		}
	}

	private boolean checkEnterAdjustAxis( Point2D_F64 p ) {
		double tol = tolerancePixels / scale;

		Point2D_F64 a0 = UtilEllipse_F64.computePoint(0,selected,null);
		Point2D_F64 a1 = UtilEllipse_F64.computePoint(Math.PI,selected,null);

		Point2D_F64 b0 = UtilEllipse_F64.computePoint(Math.PI/2.0,selected,null);
		Point2D_F64 b1 = UtilEllipse_F64.computePoint(-Math.PI/2.0,selected,null);

		if( a0.distance(p) <= tol || a1.distance(p) <= tol ) {
			mode = Mode.ADJUST_MAJOR;
			prevAngle = Math.atan2( p.y - selected.center.y , p.x - selected.center.x );
		} else if( b0.distance(p) <= tol || b1.distance(p) <= tol) {
			mode = Mode.ADJUST_MINOR;
			prevAngle = Math.atan2( p.y - selected.center.y , p.x - selected.center.x );
		} else if( selected.center.distance(p) <= tol ) {
			mode = Mode.TRANSLATE;
		} else if(Distance2D_F64.distance(selected,p) <= tol ) {
			mode = Mode.ROTATE;
			prevAngle = Math.atan2( p.y - selected.center.y , p.x - selected.center.x );
		} else {
			return false;
		}

		return true;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if( selected != null ) {
			// if it's too small just get rid of the circle
			if( selected.a < 3.0 ) {
				synchronized (list) {
					list.remove(selected);
				}
				selected = null;
				repaint();
			} else if( selected.a < selected.b ) {
				double tmp = selected.b;
				selected.b = selected.a;
				selected.a = tmp;
				selected.phi = UtilAngle.boundHalf(selected.phi + Math.PI/2.0);
				repaint();
			}
		}
		mode = Mode.IDLE;
	}

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

		double delta = 0.05;

		switch( e.getKeyCode() ) {
//			case KeyEvent.VK_S:
//				activeSet = new ArrayList<Point2D_F64>();
//				break;

//			case KeyEvent.VK_T:
//				splitAtSelected();
//				break;

			case KeyEvent.VK_NUMPAD4:
			case KeyEvent.VK_KP_LEFT:
				selected.center.x -= delta;
				break;

			case KeyEvent.VK_NUMPAD6:
			case KeyEvent.VK_KP_RIGHT:
				selected.center.x += delta;
				break;

			case KeyEvent.VK_NUMPAD2:
			case KeyEvent.VK_KP_DOWN:
				selected.center.y += delta;
				break;

			case KeyEvent.VK_NUMPAD8:
			case KeyEvent.VK_KP_UP:
				selected.center.y -= delta;
				break;

			case KeyEvent.VK_DELETE:
			case KeyEvent.VK_BACK_SPACE:
				synchronized (list) {
					list.remove(selected);
				}
				selected = null;
				break;

			case KeyEvent.VK_NUMPAD7:
				selected.center.x -= delta;
				selected.center.y -= delta;
				break;

			case KeyEvent.VK_NUMPAD9:
				selected.center.x += delta;
				selected.center.y -= delta;
				break;

			case KeyEvent.VK_NUMPAD1:
				selected.center.x -= delta;
				selected.center.y += delta;
				break;

			case KeyEvent.VK_NUMPAD3:
				selected.center.x += delta;
				selected.center.y += delta;
				break;
		}

		repaint();
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}


	public void clearPoints() {
		synchronized (list) {
			list.clear();
			selected = null;
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		Point2D_F64 p = pixelToPoint(e.getX(), e.getY());

		if( mode == Mode.DRAW_CIRCLE ) {
			selected.a = selected.b = p.distance(seedPoint)/2.0;
			selected.center.x = (p.x + seedPoint.x)/2.0;
			selected.center.y = (p.y + seedPoint.y)/2.0;
			selected.phi = UtilAngle.boundHalf(Math.atan2(p.y-seedPoint.y,p.x-seedPoint.x));
			repaint();
		} else if( mode == Mode.ADJUST_MAJOR ) {
			selected.a = selected.center.distance(p);
			adjustAngle(p);
			repaint();
		} else if( mode == Mode.ADJUST_MINOR ) {
			selected.b = selected.center.distance(p);
			adjustAngle(p);
			repaint();
		} else if( mode == Mode.TRANSLATE ) {
			selected.center.set(p);
			repaint();
		} else if( mode == Mode.ROTATE ) {
			adjustAngle(p);
			repaint();
		}
	}

	private void adjustAngle(Point2D_F64 p) {
		double angle = Math.atan2( p.y - selected.center.y , p.x - selected.center.x );
		double delta = UtilAngle.minus(angle,prevAngle);
		selected.phi = UtilAngle.boundHalf(selected.phi+delta);
		prevAngle = angle;
	}

	@Override
	public void mouseMoved(MouseEvent e) {

	}

	enum Mode {
		IDLE,
		DRAW_CIRCLE,
		ADJUST_MAJOR,
		ADJUST_MINOR,
		TRANSLATE,
		ROTATE
	}
}
