package boofcv.applications;

import boofcv.common.misc.PointFileCodec;
import boofcv.demonstrations.shapes.DetectBlackShapePanel;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.fiducial.VisualizeFiducial;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.io.UtilIO;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.geometry.UtilPoint2D_F64;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * @author Peter Abeles
 */
public class HandSelectPolygonsApp <T extends ImageGray<T>> extends DemonstrationBase {
	VisualizePanel imagePanel = new VisualizePanel();
	ControlPanel controlPanel = new ControlPanel();

	//------- Only manipulate in GUI thread
	final List<Polygon2D_F64> polygons = new ArrayList<>();
	int activeIdx=-1;
	int selectedPoint=-1;

	public HandSelectPolygonsApp() {
		super(true, false, null, ImageType.SB_U8);
		allowVideos = false;

		imagePanel.getImagePanel().addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				handleMouse(e);
			}
		});
		imagePanel.setListener(scale-> controlPanel.setZoom(scale));
		imagePanel.setPreferredSize(new Dimension(800,800));

		add(BorderLayout.WEST, controlPanel);
		add(BorderLayout.CENTER, imagePanel);
	}

	@Override
	protected void customAddToFileMenu(JMenu menuFile) {
		JMenuItem menuItemNext = new JMenuItem("Save");
		BoofSwingUtil.setMenuItemKeys(menuItemNext, KeyEvent.VK_S,KeyEvent.VK_S);
		menuItemNext.addActionListener(e -> saveToDisk(true));
		menuFile.add(menuItemNext);
	}

	@Override
	public void openNextFile() {
		saveToDisk(false);
		super.openNextFile();
	}

	private void handleMouse(MouseEvent e) {
		imagePanel.requestFocus();
		Point2D_F64 p = imagePanel.pixelToPoint(e.getX(), e.getY());
		if (SwingUtilities.isRightMouseButton(e)) {
			imagePanel.centerView(p.x, p.y);
		} else if(SwingUtilities.isLeftMouseButton(e)) {
			if( checkAndHandleClickedCorner(p.x,p.y) ) {
				imagePanel.repaint();
				return;
			}

			// nothing is selected, start a new polygon
			if( activeIdx == -1 ) {
				activeIdx = polygons.size();
				polygons.add( new Polygon2D_F64());
			}
			Polygon2D_F64 active = polygons.get(activeIdx);

			// current polygon is at max sides. start a new one
			if( active.size() == controlPanel.numSides ) {
				activeIdx = polygons.size();
				polygons.add( new Polygon2D_F64());
				active = polygons.get(activeIdx);
			}

			selectedPoint = active.size();
			active.vertexes.grow().setTo(p.x,p.y);
			imagePanel.repaint();
		}
	}

	private boolean checkAndHandleClickedCorner( double cx , double cy ) {
		double tol = 10.0/imagePanel.getScale();
		for( int i = 0; i < polygons.size(); i++ ) {
			Polygon2D_F64 polygon = polygons.get(i);
			int matched = -1;
			for (int j = 0; j < polygon.size(); j++) {
				if( polygon.get(j).distance(cx,cy) <= tol ) {
					matched = j;
					break;
				}
			}

			if( matched >= 0 ) {
				activeIdx = i;
				selectedPoint = matched;
				return true;
			}
		}
		return false;
	}

	private void saveToDisk( boolean overwrite ) {
		if( polygons.isEmpty() )
			return;
		File outputPath = createPointPath();
		if( outputPath == null )
			return;
		if( !overwrite && outputPath.exists() )
			return;

		List<List<Point2D_F64>> pointSets = new ArrayList<>();
		for( Polygon2D_F64 p : polygons ) {
			pointSets.add(p.vertexes.toList());
		}

		PointFileCodec.saveSets(outputPath.getPath(), "list of hand selected 2D points", pointSets);
		System.out.println("Saved to " + outputPath.getPath());
	}

	private File createPointPath() {
		String path = UtilIO.ensureFilePath(inputFilePath);
		if( path == null )
			return null;
		File inputFile = new File(path);
		String name = FilenameUtils.getBaseName(inputFile.getName())+".txt";
		return new File(inputFile.getParent(),name);
	}

	@Override
	protected void handleInputChange(int source, InputMethod method, int width, int height) {
		super.handleInputChange(source, method, width, height);

		polygons.clear();
		activeIdx = -1;
		selectedPoint = -1;

		File outputPath = createPointPath();
		if( outputPath != null && outputPath.exists() ) {
			List<List<Point2D_F64>> pointSets = PointFileCodec.loadSets(outputPath);

			for( List<Point2D_F64> set : pointSets ) {
				Polygon2D_F64 polygon = new Polygon2D_F64(set.size());
				polygon.vertexes.reset();
				for( Point2D_F64 p : set ) {
					polygon.vertexes.grow().setTo(p);
				}
				polygons.add( polygon );
			}
		}

		controlPanel.setImageSize(width,height);
	}

	@Override
	public void processImage(int sourceID, long frameID, BufferedImage buffered, ImageBase input) {
		BoofSwingUtil.invokeNowOrLater(()->{
			imagePanel.setImage(buffered);
			imagePanel.autoScaleAndAlign();
			imagePanel.repaint();
		});
	}

	void handleShiftUp() {
		if (activeIdx==-1) {
			for (Polygon2D_F64 p : polygons) {
				UtilPolygons2D_F64.shiftUp(p);
			}
		} else {
			UtilPolygons2D_F64.shiftUp(polygons.get(activeIdx));
		}
		imagePanel.repaint();
	}

	void handleShiftDown() {
		if (activeIdx==-1) {
			for (Polygon2D_F64 p : polygons) {
				UtilPolygons2D_F64.shiftDown(p);
			}
		} else {
			UtilPolygons2D_F64.shiftDown(polygons.get(activeIdx));
		}
		imagePanel.repaint();
	}

	void handleFlip() {
		if (activeIdx==-1) {
			for (Polygon2D_F64 p : polygons) {
				UtilPolygons2D_F64.flip(p);
			}
		} else {
			UtilPolygons2D_F64.flip(polygons.get(activeIdx));
		}
		imagePanel.repaint();
	}

	class VisualizePanel extends ImageZoomPanel {

		final Font idFont = new Font("Serif", Font.BOLD, 24);
		final Color bgColor = new Color(0, 0, 0, 130);

		public VisualizePanel() {
			setWheelScrollingEnabled(false);
			panel.addMouseWheelListener(e->{
				setScale(BoofSwingUtil.mouseWheelImageZoom(scale,e));
			});

			panel.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
				}

				@Override
				public void mouseExited(MouseEvent e) {
					setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}
			});

			addKeyListener(new KeyControls());
		}

		@Override
		protected void paintInPanel(AffineTransform tran, Graphics2D g2) {
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g2.setStroke(new BasicStroke(4));

			Point2D_F64 center = new Point2D_F64();
			for( int i = 0; i < polygons.size(); i++ ) {
				if( i == activeIdx )
					continue;
				Polygon2D_F64 polygon = polygons.get(i);
				VisualizeShapes.drawPolygon(polygon,true,scale,Color.CYAN,Color.BLUE,g2);
				UtilPoint2D_F64.mean(polygon.vertexes.toList(),center);
				VisualizeFiducial.drawLabel(center,""+i, idFont,Color.ORANGE,bgColor,g2, scale);
				Point2D_F64 p = polygon.get(0);
				VisualizeFeatures.drawPoint(g2,scale*p.x,scale*p.y,5,Color.RED,true);
			}

			if( activeIdx >= 0 ) {
				Polygon2D_F64 active = polygons.get(activeIdx);
				g2.setStroke(new BasicStroke(3.0f));
				if( active.size() >= 2 )
					VisualizeShapes.drawPolygon(active,false,scale,Color.RED,Color.BLUE,g2);
				for( int i = 0; i < active.vertexes.size; i++ ) {
					Point2D_F64 p = active.get(i);
					if( i == selectedPoint ) {
						VisualizeFeatures.drawCircle(g2, scale * p.x, scale * p.y, 7);
					} else {
						VisualizeFeatures.drawPoint(g2, scale * p.x, scale * p.y, 5, Color.RED, true);
					}
				}
			}
		}
	}

	class ControlPanel extends DetectBlackShapePanel {

		int numSides = 4;

		JSpinner spinSides = spinner(numSides,1,100,1);
		JButton bShiftUp = button("Shift Up",true, e-> handleShiftUp());
		JButton bShiftDown = button("Shift Down",true, e-> handleShiftDown());
		JButton bFlip = button("Flip",true,e->handleFlip());

		public ControlPanel() {
			selectZoom = spinner(1.0,MIN_ZOOM,MAX_ZOOM,1);

			addLabeled(imageSizeLabel,"Size");
			addLabeled(selectZoom,"Zoom");
			addLabeled(spinSides,"Num Sides");
			add(BoofSwingUtil.gridPanel(0,2,5,5,bShiftUp,bFlip,bShiftDown));
		}

		@Override
		public void controlChanged(Object source) {
			if( source == spinSides ) {
				numSides = ((Number) spinSides.getValue()).intValue();
			} else if( source == selectZoom ) {
				zoom = ((Number) selectZoom.getValue()).doubleValue();
				imagePanel.setScale(zoom);
			}
			imagePanel.repaint();
		}
	}

	class KeyControls extends KeyAdapter {
		@Override
		public void keyPressed(KeyEvent e) {
//			System.out.println("key pressed "+activeIdx+" "+selectedPoint);

			Point2D_F64 p = null;
			if( activeIdx >= 0 && selectedPoint >= 0 ) {
				p = polygons.get(activeIdx).get(selectedPoint);
			}

			double delta = 1.0/imagePanel.getScale();

			switch( e.getKeyCode() ) {
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
					if( activeIdx >= 0 ) {
						polygons.remove(activeIdx);
						activeIdx = -1;
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
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(()->{
			HandSelectPolygonsApp app = new HandSelectPolygonsApp();
			app.openFileMenuBar();
			app.display("Hand Select Polygon");
		});
	}

}
