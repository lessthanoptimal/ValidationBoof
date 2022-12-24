package boofcv.applications;

import boofcv.demonstrations.shapes.DetectBlackShapePanel;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.fiducial.VisualizeFiducial;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.io.UtilIO;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.point.Point2D_F64;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Lets you label AABB regions. AABB = axis aligned bounding boxes.
 */
public class HandSelectAabbRegionsApp extends DemonstrationBase {
	// TODO define region by click and dragging
	// TODO select a corner then adjust the region by dragging
	// TODO use keyboard to adjust corner of selected region
	// TODO assign labels to regions

	VisualizePanel imagePanel = new VisualizePanel();
	ControlPanel controlPanel = new ControlPanel();

	//------- Only manipulate in GUI thread
	final List<AABB> regions = new ArrayList<>();
	// Which region is active
	int activeIdx = -1;
	// 0 = top-left, 1 = top-right, 2 = bottom-right, 3 = bottom-left
	int selectedPoint = -1;

	public HandSelectAabbRegionsApp() {
		super(true, false, null, ImageType.SB_U8);
		allowVideos = false;

		imagePanel.getImagePanel().addMouseListener(new MouseAdapter() {
			@Override public void mousePressed( MouseEvent e ) {handleMouse(e);}
		});
		imagePanel.setListener(scale -> controlPanel.setZoom(scale));
		imagePanel.setPreferredSize(new Dimension(800, 800));

		add(BorderLayout.WEST, controlPanel);
		add(BorderLayout.CENTER, imagePanel);
	}

	@Override protected void customAddToFileMenu( JMenu menuFile ) {
		JMenuItem menuItemNext = new JMenuItem("Save");
		BoofSwingUtil.setMenuItemKeys(menuItemNext, KeyEvent.VK_S, KeyEvent.VK_S);
		menuItemNext.addActionListener(e -> saveToDisk(true));
		menuFile.add(menuItemNext);
	}

	@Override public void openFile( File file, boolean addToRecent ) {
		saveToDisk(false);
		super.openFile(file, addToRecent);
	}

	@Override public void openNextFile() {
		saveToDisk(false);
		super.openNextFile();
	}

	private void handleMouse( MouseEvent e ) {
		imagePanel.requestFocus();
		Point2D_F64 p = imagePanel.pixelToPoint(e.getX(), e.getY());
		if (SwingUtilities.isRightMouseButton(e)) {
			imagePanel.centerView(p.x, p.y);
		} else if (SwingUtilities.isLeftMouseButton(e)) {
			if (checkAndHandleClickedCorner(p.x, p.y)) {
				imagePanel.repaint();
				return;
			}

			// nothing is selected, start a new polygon
			if (activeIdx == -1) {
				selectedPoint = 2; // bottom right corner will be active
				activeIdx = regions.size();
				var active = new AABB();
				active.tl.setTo(p);
				active.br.setTo(p);
				regions.add(active);
			} else {
				AABB active = regions.get(activeIdx);
				active.br.setTo(p);
				// deactivate so that another can be labeled immediately
				activeIdx = -1;
			}

			imagePanel.repaint();
		}
	}

	private boolean checkAndHandleClickedCorner( double cx, double cy ) {
		double tol = 10.0/imagePanel.getScale();
		for (int regionIdx = 0; regionIdx < regions.size(); regionIdx++) {
			AABB polygon = regions.get(regionIdx);
			int matched = -1;
			for (int cornerIdx = 0; cornerIdx < 4; cornerIdx++) {
				if (polygon.distanceCorner(cornerIdx, cx, cy) <= tol) {
					matched = cornerIdx;
					break;
				}
			}

			if (matched >= 0) {
				activeIdx = regionIdx;
				selectedPoint = matched;
				return true;
			}
		}
		return false;
	}

	private void saveToDisk( boolean overwrite ) {
		if (regions.isEmpty())
			return;
		File outputPath = createPointPath();
		if (outputPath == null)
			return;
		if (!overwrite && outputPath.exists())
			return;

		try (var out = new BufferedWriter(new FileWriter(outputPath))) {
			out.write("# AABB Labels\n");
			for (AABB region : regions) {
				out.write(String.format("%.10f %.10f %.10f %.10f\n", region.tl.x, region.tl.y, region.br.x, region.br.y));
			}
		} catch (IOException e) {
			BoofSwingUtil.warningDialog(this, e);
		}

		System.out.println("Saved to " + outputPath.getPath());
	}

	private File createPointPath() {
		String path = UtilIO.ensureFilePath(inputFilePath);
		if (path == null)
			return null;
		File inputFile = new File(path);
		String name = FilenameUtils.getBaseName(inputFile.getName()) + ".txt";
		return new File(inputFile.getParent(), name);
	}

	@Override protected void handleInputChange( int source, InputMethod method, int width, int height ) {
		super.handleInputChange(source, method, width, height);
		BoofSwingUtil.checkGuiThread();

		regions.clear();
		activeIdx = -1;
		selectedPoint = -1;

		File outputPath = createPointPath();
		if (outputPath != null && outputPath.exists()) {
			List<AABB> regions = new ArrayList<>();
			try (var reader = new BufferedReader(new FileReader(outputPath))) {
				String line = reader.readLine();
				while (line != null) {
					if (line.length() == 0 || line.charAt(0) == '#') {
						line = reader.readLine();
						continue;
					}
					String[] words = line.split(" ");
					if ( words.length != 4)
						throw new IOException("Expected 4 words not "+words.length);

					var aabb = new AABB();
					aabb.tl.x = Double.parseDouble(words[0]);
					aabb.tl.y = Double.parseDouble(words[1]);
					aabb.br.x = Double.parseDouble(words[2]);
					aabb.br.y = Double.parseDouble(words[3]);
					regions.add(aabb);
					line = reader.readLine();
				}
				this.regions.clear();
				this.regions.addAll(regions);
			} catch (IOException e) {
				BoofSwingUtil.warningDialog(this, e);
			}
		}

		controlPanel.setImageSize(width, height);
	}

	@Override public void processImage( int sourceID, long frameID, BufferedImage buffered, ImageBase input ) {
		BoofSwingUtil.invokeNowOrLater(() -> {
			imagePanel.setImage(buffered);
			imagePanel.autoScaleAndAlign();
			imagePanel.repaint();
		});
	}

	class VisualizePanel extends ImageZoomPanel {

		final Font idFont = new Font("Serif", Font.BOLD, 24);
		final Color bgColor = new Color(0, 0, 0, 130);
		Ellipse2D.Double c = new Ellipse2D.Double();
		BasicStroke thinStroke = new BasicStroke(3.0f);
		BasicStroke thickStroke = new BasicStroke(5.0f);

		public VisualizePanel() {
			setWheelScrollingEnabled(false);
			panel.addMouseWheelListener(e -> {
				setScale(BoofSwingUtil.mouseWheelImageZoom(scale, e));
			});

			panel.addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered( MouseEvent e ) {
					setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
				}

				@Override public void mouseExited( MouseEvent e ) {
					setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}
			});

			addKeyListener(new KeyControls());
		}

		@Override protected void paintInPanel( AffineTransform tran, Graphics2D g2 ) {
			BoofSwingUtil.antialiasing(g2);

			g2.setStroke(new BasicStroke(4));

			var center = new Point2D_F64();
			for (int i = 0; i < regions.size(); i++) {
				if (i == activeIdx)
					continue;
				AABB polygon = regions.get(i);
				drawAABB(polygon, scale, Color.CYAN, Color.BLUE, g2);

				polygon.center(center);
				VisualizeFiducial.drawLabel(center, "" + i, idFont, Color.ORANGE, bgColor, g2, scale);
//				Point2D_F64 p = polygon.get(0);
//				VisualizeFeatures.drawPoint(g2, scale*p.x, scale*p.y, 5, Color.RED, true);
			}

			if (activeIdx >= 0) {
				AABB active = regions.get(activeIdx);
				g2.setStroke(thinStroke);
				drawAABB(active, scale, Color.RED, Color.BLUE, g2);
			}
		}
	}

	public static void drawAABB( AABB region, double scale, Color color0, Color colorOthers, Graphics2D g2 ) {
		var path = new Path2D.Double();
		definePathAabb(region, scale, path);
		g2.setColor(colorOthers);
		g2.draw(path);

		definePathAabb(region, scale, path);
		g2.setColor(color0);
		g2.draw(path);
	}

	private static void definePathAabb( AABB region, double scale, Path2D.Double path ) {
		path.reset();
		path.moveTo(scale*region.tl.x, scale*region.tl.y);
		path.lineTo(scale*region.br.x, scale*region.tl.y);
		path.lineTo(scale*region.br.x, scale*region.br.y);
		path.lineTo(scale*region.tl.x, scale*region.br.y);
		path.lineTo(scale*region.tl.x, scale*region.tl.y);
	}

	class ControlPanel extends DetectBlackShapePanel {

		public ControlPanel() {
			selectZoom = spinner(1.0, MIN_ZOOM, MAX_ZOOM, 1);

			addLabeled(imageSizeLabel, "Size");
			addLabeled(selectZoom, "Zoom");
		}

		@Override public void controlChanged( Object source ) {
			if (source == selectZoom) {
				zoom = ((Number)selectZoom.getValue()).doubleValue();
				imagePanel.setScale(zoom);
			}
			imagePanel.repaint();
		}
	}

	class KeyControls extends KeyAdapter {
		@Override public void keyPressed( KeyEvent e ) {
//			System.out.println("key pressed "+activeIdx+" "+selectedPoint);

//			Point2D_F64 p = null;
//			if (activeIdx >= 0 && selectedPoint >= 0) {
//				p = regions.get(activeIdx).get(selectedPoint);
//			}
//
//			double delta = 1.0/imagePanel.getScale();
//
//			switch (e.getKeyCode()) {
//				case KeyEvent.VK_NUMPAD4, KeyEvent.VK_KP_LEFT -> {
//					if (p != null)
//						p.x -= delta;
//				}
//				case KeyEvent.VK_NUMPAD6, KeyEvent.VK_KP_RIGHT -> {
//					if (p != null)
//						p.x += delta;
//				}
//				case KeyEvent.VK_NUMPAD2, KeyEvent.VK_KP_DOWN -> {
//					if (p != null)
//						p.y += delta;
//				}
//				case KeyEvent.VK_NUMPAD8, KeyEvent.VK_KP_UP -> {
//					if (p != null)
//						p.y -= delta;
//				}
//				case KeyEvent.VK_DELETE, KeyEvent.VK_BACK_SPACE -> {
//					if (activeIdx >= 0) {
//						regions.remove(activeIdx);
//						activeIdx = -1;
//					}
//				}
//				case KeyEvent.VK_NUMPAD7 -> {
//					if (p != null) {
//						p.x -= delta;
//						p.y -= delta;
//					}
//				}
//				case KeyEvent.VK_NUMPAD9 -> {
//					if (p != null) {
//						p.x += delta;
//						p.y -= delta;
//					}
//				}
//				case KeyEvent.VK_NUMPAD1 -> {
//					if (p != null) {
//						p.x -= delta;
//						p.y += delta;
//					}
//				}
//				case KeyEvent.VK_NUMPAD3 -> {
//					if (p != null) {
//						p.x += delta;
//						p.y += delta;
//					}
//				}
//			}

			repaint();
		}
	}

	private static class AABB {
		// Top Left
		Point2D_F64 tl = new Point2D_F64();
		// Bottom Right
		Point2D_F64 br = new Point2D_F64();

		public double distanceCorner( int corner, double cx, double cy ) {
			double x, y;
			switch (corner) {
				case 0 -> {
					x = tl.x;
					y = tl.y;
				}
				case 1 -> {
					x = br.x;
					y = tl.y;
				}
				case 2 -> {
					x = br.x;
					y = br.y;
				}
				case 3 -> {
					x = tl.x;
					y = br.y;
				}
				default -> throw new RuntimeException("Unknown corner");
			}
			return UtilPoint2D_F64.distance(x, y, cx, cy);
		}

		public void center( Point2D_F64 c ) {
			c.x = (tl.x + br.x)/2.0;
			c.y = (tl.y + br.y)/2.0;
		}
	}

	public static void main( String[] args ) {
		SwingUtilities.invokeLater(() -> {
			var app = new HandSelectAabbRegionsApp();
			app.openFileMenuBar();
			app.display("Hand Select Polygon");
		});
	}
}
