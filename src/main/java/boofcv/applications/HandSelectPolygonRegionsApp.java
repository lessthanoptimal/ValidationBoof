package boofcv.applications;

import boofcv.alg.color.ColorHsv;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.io.UtilIO;
import boofcv.io.image.LabeledImagePolygonCodec;
import boofcv.io.image.PolygonRegion;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageDimension;
import georegression.geometry.PolygonInfo;
import georegression.geometry.UtilPoint2D_F64;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.metric.ClosestPoint2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.struct.DogArray;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Selects and colorizes polygon regions in an image. Each pixel can only belong to a single polygon.
 *
 * @author Peter Abeles
 */
public class HandSelectPolygonRegionsApp extends StillImageLabelingBase {
    // TODO allow some polygons to share the same ID. Select and click share id?
    // TODO save results as a polygon file and a labeled image
    // TODO Re-number a region

    final VisualizePanel imagePanel = new VisualizePanel();
    final ControlPanel controls = new ControlPanel();

    //----------------------------------------------------
    // Owned by the lock
    final Object lock = new Object();
    // local copy of user selected polygons
    final DogArray<Region> polygons = new DogArray<>(Region::new, Region::reset);
    int selectedPolygonIdx = -1;
    int selectedVertexIdx = -1;
    int regionsCreated = 0;
    boolean shiftDown = false;
    //----------------------------------------------------

    //----------------------------------------------------
    // Owned by GUI thread
    PolygonInfo polygonInfo = new PolygonInfo();
    //----------------------------------------------------

    boolean createNewPolygon=false;

    public HandSelectPolygonRegionsApp(File openFile) {
        initialize(openFile);

        // Add mouse controls to the image panel
        HandleMouse handleMouse = new HandleMouse();
        imagePanel.getImagePanel().addMouseListener(handleMouse);
        imagePanel.getImagePanel().addMouseMotionListener(handleMouse);
        imagePanel.addKeyListener(new KeyControls());
        imagePanel.setWheelScrollingEnabled(false);
    }

    @Override protected void initializeGui(JComponent panel) {
        panel.add(BorderLayout.CENTER,imagePanel);
        panel.add(BorderLayout.WEST,controls);
        setImagePanel(imagePanel);
    }

    @Override public void process(File file, BufferedImage image) {
        clearLabels();
        SwingUtilities.invokeLater(()->{
            imagePanel.setImage(image);imagePanel.repaint();imagePanel.requestFocus();
            controls.setImageSize(image.getWidth(), image.getHeight());

            // See if there are previously labeled regions saved to disk
            final File polygonFile = new File(file.getParent(),getPolygonFileName(file));
            if (polygonFile.exists()) {
                try {
                    FileInputStream input = new FileInputStream(polygonFile);
                    ImageDimension imageShape = new ImageDimension();
                    DogArray<PolygonRegion> foundRegions = new DogArray<>(PolygonRegion::new);
                    LabeledImagePolygonCodec.decode(input, imageShape, foundRegions);
                    input.close();
                    // sanity check
                    if (imageShape.getWidth() != image.getWidth() || imageShape.getHeight() !=image.getHeight())
                        throw new IOException("saved regions doesn't match image size.");
                    foundRegions.forEach(r->polygons.grow().setTo(r));

                    // Update regionsCreated so that it starts after the last ID
                    updatedRegionsCreated();
                } catch (IOException e) {
                    BoofSwingUtil.warningDialog(gui,e);
                }
            }
        });
    }

    /**
     * This makes sure a reasonable ID is applied to new IDs after the regions have been modified.
     */
    private void updatedRegionsCreated() {
        regionsCreated = -1;
        for (int i = 0; i < polygons.size; i++) {
            regionsCreated = Math.max(regionsCreated, polygons.get(i).regionID);
        }
    }

    @Override public void openNextImage() {
        final File file = this.inputFile;
        if (file==null) {
            System.err.println("inputFile is null");
            return;
        }

        // Save polygons by default when going to the next image
        if (!polygons.isEmpty()) {
            save();
        }

        // Sort the files by their name
        List<String> files = UtilIO.listAll(file.getParent());
        Collections.sort(files);

        // Find the current file in the list
        int index = files.indexOf(file.getAbsolutePath());
        if (index<0) {
            System.err.println("Not child of parent?? index="+index);
            return;
        }

        while (++index < files.size()) {
            File f = new File(files.get(index));
            if (!UtilImageIO.isImage(f))
                continue;
            openImage(new File(files.get(index)),true);
            return;
        }
        System.err.println("Failed to find a next valid image. index="+index+" size="+files.size());
    }

    @Override public String getApplicationName() {
        return "Polygon Region Labels";
    }

    @Override public void setScale(double scale) {

    }

    @Override public void save() {
        System.out.println("Saving...");
        final File file = this.inputFile;
        if (file==null) {
            System.err.println("inputFile is null");
            return;
        }
        BufferedImage image = this.image;
        if (image==null) {
            System.err.println("BufferedImage 'image' is null");
            return;
        }

        final File outputFile = new File(file.getParent(),getPolygonFileName(file));
        System.out.println("Saving to "+outputFile.getAbsolutePath());

        try {
            FileOutputStream output = new FileOutputStream(outputFile);
            LabeledImagePolygonCodec.encode((List) polygons.toList(), image.getWidth(), image.getHeight(), output);
            output.close();
        } catch (IOException e) {
            BoofSwingUtil.warningDialog(gui,e);
        }
    }

    private String getPolygonFileName(File file) {
        return FilenameUtils.getBaseName(file.getName())+"_polygons.txt";
    }

    @Override public void clearLabels() {
        synchronized (lock) {
            polygons.reset();
            selectedPolygonIdx = -1;
            selectedVertexIdx = -1;
            regionsCreated = 0;
        }
        imagePanel.repaint();
    }

    private void doDeleteSelected() {
        BoofSwingUtil.checkGuiThread();

        synchronized (lock) {
            if (selectedPolygonIdx<0)
                return;
            polygons.removeSwap(selectedPolygonIdx);
            selectedPolygonIdx = -1;
            selectedVertexIdx = -1;
            imagePanel.repaint();
        }
    }

    private void updateInvalidFlag(Region region ) {
        BoofSwingUtil.checkGuiThread();

        if (region.polygon.size() <= 3) {
            region.invalid = false;
            return;
        }

        UtilPolygons2D_F64.isSimple(region.polygon, polygonInfo, 1e-8);
        region.invalid = polygonInfo.type == PolygonInfo.Type.COMPLEX;
    }

    // TODO use version in GeoRegression when the version is upgraded
    public static int findClosestIdx(double x , double y, List<Point2D_F64> pts , double tol ) {
        double bestDist = Double.MAX_VALUE;
        int best = -1;

        for (int i = 0; i < pts.size(); i++) {
            double dist = pts.get(i).distance2(x,y);
            if (dist<bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        if (bestDist <= tol*tol)
            return best;
        return -1;
    }

    /** If the user clicked on a line it will split the line at the closest point. true if handled. */
    public boolean clickSpitLine(double x, double y, Polygon2D_F64 polygon, double tol) {
        if (polygon.size()<=1)
            return false;

        Point2D_F64 target = new Point2D_F64(x,y);
        Point2D_F64 closestPt = new Point2D_F64();
        LineParametric2D_F64 line = new LineParametric2D_F64();
        for (int i = 0, j = polygon.size()-1; i < polygon.size(); j=i,i++) {
            line.p.setTo(polygon.get(j));
            line.slope.minus(polygon.get(i), polygon.get(j));
            double t = ClosestPoint2D_F64.closestPointT(line, target);
            // see if it intersected between the two points
            if (t < 0.0 || t > 1.0)
                continue;
            line.getPointOnLine(t, closestPt);
            if (closestPt.distance2(target) > tol*tol)
                continue;

            // insert the point. since a grow array is used this isn't trivial
            polygon.vertexes.grow();
            for (int k = polygon.size()-1; k > i; k--) {
                polygon.get(k).setTo(polygon.get(k-1));
            }
            polygon.get(i).setTo(target);

            // make the new point the selected point
            selectedVertexIdx = i;
            return true;
        }
        return false;
    }

    /** Attempts to generate easily distinguished colors for each shape */
    public int colorOfShape( int regionID ) {
        final int period = 20;
        regionID = (regionID-1)*3; // -1 because region IDs start at 1
        double hue = 2.0*Math.PI*((((regionID%period)+(regionID/period)*0.2)/(double)period)%1.0);
        double sat = (((regionID+period*2-1)%(period*2))/(double)(period*2))/2.0+0.5;

        int alpha = 255 - controls.translucent*255/100;
        return (ColorHsv.hsvToRgb(hue,sat,255)&0x00FFFFFF) | (alpha << 24);
    }

    /** Sees if hte point is inside any of the polygons. If so it returns the first one */
    private int findInside( double x, double y ) {
        Point2D_F64 testPoint = new Point2D_F64(x,y);
        for (int i = 0; i < polygons.size; i++) {
            Region r = polygons.get(i);
            if (r.size() < 3) // work around for a bug in GeoRegression. This should be fixed in 0.24
                continue;
            if (Intersection2D_F64.containsConcave(r.polygon,testPoint)) {
                return i;
            }
        }

        return -1;
    }

    public class VisualizePanel extends ImageZoomPanel implements MouseWheelListener {
        Line2D.Double line = new Line2D.Double();
        Ellipse2D.Double ellipse = new Ellipse2D.Double();
        Path2D path = new java.awt.geom.Path2D.Double();
        BasicStroke stroke = new BasicStroke(5.0f);
        TexturePaint invalidTexture;

        Point2D_F64 center = new Point2D_F64();

        public VisualizePanel() {
            BufferedImage texture = new BufferedImage(10,10,BufferedImage.TYPE_INT_ARGB);
            for (int i = 0; i < 10; i++) {
                texture.setRGB(i,i,0xFFFFFFFF);
                texture.setRGB(i,9-i,0xFFFFFFFF);
            }
            invalidTexture = new TexturePaint(texture, new Rectangle2D.Double(0,0,10,10));

            getImagePanel().addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR)); }
                @Override public void mouseExited(MouseEvent e) { setCursor(new Cursor(Cursor.DEFAULT_CURSOR)); }
            });
            addMouseWheelListener(this);
        }

        @Override
        protected void paintInPanel(AffineTransform tran, Graphics2D g2 ) {
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Paint defaultPaint = g2.getPaint();

            synchronized (lock) {
                g2.setStroke(stroke);
                for (int polygonIdx = 0; polygonIdx < polygons.size; polygonIdx++) {
                    Region pr = polygons.get(polygonIdx);
                    g2.setColor(new Color(colorOfShape(pr.regionID),true));
                    if (pr.invalid)
                        g2.setPaint(invalidTexture);
                    renderPolygon(g2, pr.polygon, true);
                    if (pr.invalid)
                        g2.setPaint(defaultPaint);
                    renderLabel(g2, pr.regionID, pr.polygon);

                    // Draw a border around the selected polygon
                    if (polygonIdx==selectedPolygonIdx && pr.size()>=3) {
                        g2.setColor(Color.WHITE);
                        renderPolygon(g2, pr.polygon, false);
                        g2.setColor(Color.BLACK);
                        renderPolygonVertexes(g2, pr.polygon);
                    }

                    // Emphasize the selected point on the polygon
                    if (polygonIdx==selectedPolygonIdx &&
                            selectedVertexIdx >=0 && selectedVertexIdx < pr.size()) {
                        g2.setColor(Color.BLUE);
                        Point2D_F64 p = pr.polygon.get(selectedVertexIdx);
                        double r = 6.0;
                        ellipse.setFrame(scale*p.x-r,  scale*p.y-r,r*2+1, r*2+1 );
                        g2.draw(ellipse);
                    }
                }
            }
        }

        /**
         * Draws a polygon and handles special cases of 0,1,2 points
         */
        void renderPolygon(Graphics2D g2 , Polygon2D_F64 polygon, boolean filled ) {
            if (polygon.size()==0) {
                return;
            }
            if (polygon.size()==1) {
                Point2D_F64 p = polygon.get(0);
                double r = 3.0;
                ellipse.setFrame(scale*p.x-r,  scale*p.y-r,r*2+1, r*2+1 );
                g2.fill(ellipse);
            } else if (polygon.size()==2) {
                Point2D_F64 p0 = polygon.get(0);
                Point2D_F64 p1 = polygon.get(1);

                line.setLine( scale*p0.x, scale*p0.y,
                        scale*p1.x, scale*p1.y);
                g2.draw(line);
            } else {
                Point2D_F64 p = polygon.get(0);
                path.reset();
                path.moveTo(p.x * scale, p.y * scale);
                for(int i = 1; i <= polygon.size(); ++i) {
                    p = polygon.get(i % polygon.size());
                    path.lineTo(p.x * scale,p.y * scale);
                }
                if (filled)
                    g2.fill(path);
                else
                    g2.draw(path);
            }
        }

        void renderPolygonVertexes(Graphics2D g2 , Polygon2D_F64 polygon) {
            double r = 1.0;
            for(int i = 0; i < polygon.size(); ++i) {
                Point2D_F64 p = polygon.get(i);
                ellipse.setFrame(p.x*scale-r, p.y*scale-r, 2*r+1, 2*r+1);
                g2.fill(ellipse);
            }
        }

        void renderLabel(Graphics2D g2 , int regionID, Polygon2D_F64 polygon ) {
            if (polygon.vertexes.size<3)
                return;

            UtilPoint2D_F64.mean(polygon.vertexes.toList(),center);
            g2.setColor(Color.BLACK);
            g2.drawString(""+regionID, (float)(center.x*scale), (float)(center.y*scale));
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            setScale(BoofSwingUtil.mouseWheelImageZoom(scale,e));
        }

        @Override
        public synchronized void setScale(double scale) {
            super.setScale(scale);
            controls.setZoom(scale);
        }
    }

    public class HandleMouse extends MouseAdapter {
        // indicates which mouse button is being pressed
        int mouseButtonDown=-1;

        @Override public void mousePressed(MouseEvent e) {
            // request focus so that key commands are passed to this panel
            imagePanel.requestFocus();

            double x = e.getX()/imagePanel.getScale();
            double y = e.getY()/imagePanel.getScale();

            mouseButtonDown = 0;

            // right click can be used to center the view
            if (BoofSwingUtil.isRightClick(e)) {
                mouseButtonDown = 3;
                imagePanel.centerView(x,y);
                return;
            }

            synchronized (lock) {
                Region selected;
                if (selectedPolygonIdx==-1 || createNewPolygon || shiftDown) {
                    // If a polygon was created that has less than 3 sides, remove it
                    if (selectedPolygonIdx!=-1) {
                        Region p = polygons.get(selectedPolygonIdx);
                        if (p.size() < 3) {
                            polygons.removeSwap(selectedPolygonIdx);
                        }
                    }
                    // Reset variables to prevent smalling
                    createNewPolygon = false;
                    shiftDown = false;
                    // Nothing is selected. Create a new polygon
                    selectedPolygonIdx = polygons.size;
                    selected = polygons.grow();
                    // 1 + because region id=0 is reserved for the background
                    selected.regionID = 1+regionsCreated++;
                } else {
                    selected = polygons.get(selectedPolygonIdx);
                }

                // See if the user clicked on any corners on the polygon
                double tol = 10.0/imagePanel.getScale();
                int matchIdx = findClosestIdx(x,y,selected.polygon.vertexes.toList(), tol);
                if (matchIdx==-1) {
                    // see if the user wants to split a line and add a new point there
                    if (!clickSpitLine(x,y,selected.polygon, tol)) {
                        // See if it's inside another polygon and make that one active
                        int insideIdx = findInside(x, y);
                        if (insideIdx != -1) {
                            selectedPolygonIdx = insideIdx;
                            selectedVertexIdx = -1;
                        } else {
                            // Add a new point to the end of the polygon
                            selectedVertexIdx = selected.size();
                            selected.polygon.vertexes.grow().setTo(x, y);

                            // Don't let the user create an invalid polygon
                            UtilPolygons2D_F64.isSimple(selected.polygon, polygonInfo, 1e-8);
                            if (selected.size() > 2 && polygonInfo.type == PolygonInfo.Type.COMPLEX) {
                                selected.polygon.vertexes.removeTail();
                                selectedVertexIdx = -1;
                            }
                        }
                    }
                } else {
                    // change the selected vertex to what the user just clicked
                    selectedVertexIdx = matchIdx;
                }
            }
            imagePanel.repaint();
        }

        @Override public void mouseDragged(MouseEvent e) {
            // Only drag corners when it's the left mouse button
            if (mouseButtonDown != 0)
                return;

            synchronized (lock) {
                if (selectedPolygonIdx==-1 || selectedVertexIdx==-1)
                    return;

                // If the user has a vertex selected and is dragging, move that vertex
                Region selected = polygons.get(selectedPolygonIdx);
                Point2D_F64 vertex = selected.polygon.get(selectedVertexIdx);

                double x = e.getX()/imagePanel.getScale();
                double y = e.getY()/imagePanel.getScale();
                vertex.setTo(x,y);

                updateInvalidFlag(selected);

                imagePanel.repaint();
            }
        }
    }

    class ControlPanel extends StandardAlgConfigPanel {
        public double zoom = 1.0D;
        public int translucent = 30;

        protected JSpinner selectZoom = spinner(zoom, BoofSwingUtil.MIN_ZOOM, BoofSwingUtil.MAX_ZOOM, 0.2 );
        protected JLabel imageSizeLabel = new JLabel();
        protected JButton bNew = button("Start New", true, e->createNewPolygon=true);
        protected JButton bDelete = button("Delete", true, e->doDeleteSelected());
        JSlider sliderTranslucent = new JSlider(JSlider.HORIZONTAL, 0, 100, translucent);

        public ControlPanel() {
            sliderTranslucent.setMaximumSize(new Dimension(120, 26));
            sliderTranslucent.setPreferredSize(sliderTranslucent.getMaximumSize());
            sliderTranslucent.addChangeListener(this);

            addLabeled(imageSizeLabel,"Image Size");
            addLabeled(selectZoom,"Zoom");
            addLabeled(sliderTranslucent,"Opaque");
            addAlignCenter(bNew);
            addAlignCenter(bDelete);
        }

        public void setZoom(double _zoom) {
            _zoom = Math.max(BoofSwingUtil.MIN_ZOOM, _zoom);
            _zoom = Math.min(BoofSwingUtil.MAX_ZOOM, _zoom);
            if (_zoom != this.zoom) {
                this.zoom = _zoom;
                BoofSwingUtil.invokeNowOrLater(() -> this.selectZoom.setValue(this.zoom));
            }
        }

        public void setImageSize(int width, int height) {
            BoofSwingUtil.invokeNowOrLater(() -> this.imageSizeLabel.setText(width + " x " + height));
        }

        @Override
        public void controlChanged(Object source) {
            if (source==selectZoom) {
                this.zoom = ((Number)selectZoom.getValue()).doubleValue();
                imagePanel.setScale(this.zoom);
            } else if (source == sliderTranslucent) {
                translucent = ((Number)sliderTranslucent.getValue()).intValue();
                imagePanel.repaint();
            }
        }
    }

    class KeyControls extends KeyAdapter {
        @Override public void keyPressed(KeyEvent e) {
            // See if a vertex on a polygon is selected
            Point2D_F64 p;
            Region region;
            synchronized (lock) {
                shiftDown = e.isShiftDown();

                if (selectedPolygonIdx == -1 || selectedVertexIdx == -1)
                    return;

                region = polygons.get(selectedPolygonIdx);

                // See if a remove point was requested
                int kc = e.getKeyCode();
                boolean delete = kc == KeyEvent.VK_DELETE || kc == KeyEvent.VK_BACK_SPACE;
                if (delete) {
                    region.polygon.vertexes.remove(selectedVertexIdx);
                    updateInvalidFlag(region);
                    updatedRegionsCreated();
                    imagePanel.repaint();
                    return;
                }
                p = region.polygon.get(selectedVertexIdx);
            }

            // Move the point by one pixel at the current scale/zoom
            double delta = 1.0 / imagePanel.getScale();

            switch (e.getKeyCode()) {
                case KeyEvent.VK_NUMPAD4:
                case KeyEvent.VK_KP_LEFT:
                case KeyEvent.VK_A:
                    p.x -= delta;
                    break;

                case KeyEvent.VK_NUMPAD6:
                case KeyEvent.VK_KP_RIGHT:
                case KeyEvent.VK_D:
                    p.x += delta;
                    break;

                case KeyEvent.VK_NUMPAD2:
                case KeyEvent.VK_KP_DOWN:
                case KeyEvent.VK_S:
                    p.y += delta;
                    break;

                case KeyEvent.VK_NUMPAD8:
                case KeyEvent.VK_KP_UP:
                case KeyEvent.VK_W:
                    p.y -= delta;
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
            updateInvalidFlag(region);
            repaint();
        }

        @Override public void keyReleased(KeyEvent e) {
            synchronized (lock) {
                shiftDown = e.isShiftDown();
            }
        }
    }

    /**
     * Describes a polygon and its ID
     */
    class Region extends PolygonRegion {
        // Meta data used for visualization
        public boolean invalid;
        public boolean overlap;

        public void setTo(Region src) {
            super.setTo(src);
            this.invalid = src.invalid;
            this.overlap = src.overlap;
        }

        public void reset() {
            super.reset();
            invalid = false;
            overlap = false;
        }

        public int size() {
            return polygon.size();
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(()->new HandSelectPolygonRegionsApp(null));
    }
}
