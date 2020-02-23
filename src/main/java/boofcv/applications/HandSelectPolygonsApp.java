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

	final List<Polygon2D_F64> polygons = new ArrayList<>();

	final Polygon2D_F64 active = new Polygon2D_F64();

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
			active.vertexes.grow().set(p.x,p.y);
			imagePanel.repaint();

			if( active.size() == controlPanel.numSides ) {
				polygons.add( active.copy() );
				active.vertexes.reset();
			}
		}
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
		active.vertexes.reset();

		File outputPath = createPointPath();
		if( outputPath != null && outputPath.exists() ) {
			List<List<Point2D_F64>> pointSets = PointFileCodec.loadSets(outputPath.getPath());

			for( List<Point2D_F64> set : pointSets ) {
				Polygon2D_F64 polygon = new Polygon2D_F64(set.size());
				polygon.vertexes.reset();
				for( Point2D_F64 p : set ) {
					polygon.vertexes.grow().set(p);
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

			panel.addKeyListener(new KeyControls());
		}

		@Override
		protected void paintInPanel(AffineTransform tran, Graphics2D g2) {
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g2.setStroke(new BasicStroke(4));

			Point2D_F64 center = new Point2D_F64();
			for( int i = 0; i < polygons.size(); i++ ) {
				Polygon2D_F64 polygon = polygons.get(i);
				VisualizeShapes.drawPolygon(polygon,true,scale,Color.CYAN,Color.BLUE,g2);
				UtilPoint2D_F64.mean(polygon.vertexes.toList(),center);
				VisualizeFiducial.drawLabel(center,""+i, idFont,Color.ORANGE,bgColor,g2, scale);
			}

			if( active.size() > 1 )
				VisualizeShapes.drawPolygon(active,false,scale,Color.RED,Color.BLUE,g2);
			for( Point2D_F64 p : active.vertexes.toList() ) {
				VisualizeFeatures.drawPoint(g2,scale*p.x,scale*p.y,5,Color.RED,true);
			}
		}
	}

	class ControlPanel extends DetectBlackShapePanel {

		int numSides = 4;

		JSpinner spinSides = spinner(numSides,1,100,1);

		public ControlPanel() {
			selectZoom = spinner(1.0,MIN_ZOOM,MAX_ZOOM,1);

			addLabeled(imageSizeLabel,"Size");
			addLabeled(selectZoom,"Zoom");
			addLabeled(spinSides,"Num Sides");
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

//			Point2D_F64 p = selected;
//
//			double delta = 1.0/scale;
//
//			switch( e.getKeyCode() ) {
//				case KeyEvent.VK_S:
//					activeSet = new ArrayList<>();
//					break;
//
//				case KeyEvent.VK_T:
//					splitAtSelected();
//					break;
//
//				case KeyEvent.VK_NUMPAD4:
//				case KeyEvent.VK_KP_LEFT:
//					if( p != null )
//						p.x -= delta;
//					break;
//
//				case KeyEvent.VK_NUMPAD6:
//				case KeyEvent.VK_KP_RIGHT:
//					if( p != null )
//						p.x += delta;
//					break;
//
//				case KeyEvent.VK_NUMPAD2:
//				case KeyEvent.VK_KP_DOWN:
//					if( p != null )
//						p.y += delta;
//					break;
//
//				case KeyEvent.VK_NUMPAD8:
//				case KeyEvent.VK_KP_UP:
//					if( p != null )
//						p.y -= delta;
//					break;
//
//				case KeyEvent.VK_DELETE:
//				case KeyEvent.VK_BACK_SPACE:
//					if( selected != null ) {
//						for (List<Point2D_F64> set : pointSets) {
//							if (set.remove(selected)) {
//								break;
//							}
//						}
//						selected = null;
//					}
//					break;
//
//				case KeyEvent.VK_NUMPAD7:
//					if( p != null ) {
//						p.x -= delta;
//						p.y -= delta;
//					}
//					break;
//
//				case KeyEvent.VK_NUMPAD9:
//					if( p != null ) {
//						p.x += delta;
//						p.y -= delta;
//					}
//					break;
//
//				case KeyEvent.VK_NUMPAD1:
//					if( p != null ) {
//						p.x -= delta;
//						p.y += delta;
//					}
//					break;
//
//				case KeyEvent.VK_NUMPAD3:
//					if( p != null ) {
//						p.x += delta;
//						p.y += delta;
//					}
//					break;
//			}

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
