package com.expensetracker.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Renders the Lucide SVG icons in /icons (24x24 grid, stroke style) with Java2D. */
final class SvgIcons {
    private static final Map<String, List<Shape>> CACHE = new HashMap<>();

    private SvgIcons() {
    }

    static void paint(Graphics2D g, String type, double x, double y, double size, Color color) {
        List<Shape> shapes = shapes(type);
        if (shapes.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.translate(x, y);
        double s = size / 24.0;
        g2.scale(s, s);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (Shape shape : shapes) {
            g2.draw(shape);
        }
        g2.dispose();
    }

    private static synchronized List<Shape> shapes(String type) {
        return CACHE.computeIfAbsent(type, SvgIcons::load);
    }

    private static List<Shape> load(String type) {
        String resource = "/icons/" + type + ".svg";
        try (java.io.InputStream in = LandingPage.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing icon resource: " + resource);
            }
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            org.w3c.dom.Document doc = factory.newDocumentBuilder().parse(in);
            List<Shape> shapes = new ArrayList<>();
            org.w3c.dom.NodeList nodes = doc.getDocumentElement().getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                if (nodes.item(i) instanceof org.w3c.dom.Element element) {
                    Shape shape = toShape(element);
                    if (shape != null) {
                        shapes.add(shape);
                    }
                }
            }
            return shapes;
        } catch (IOException | org.xml.sax.SAXException | javax.xml.parsers.ParserConfigurationException e) {
            throw new IllegalStateException("Unable to load icon resource: " + resource, e);
        }
    }

    private static Shape toShape(org.w3c.dom.Element e) {
        switch (e.getTagName()) {
            case "path":
                return parsePath(e.getAttribute("d"));
            case "circle": {
                double r = num(e, "r");
                return new Ellipse2D.Double(num(e, "cx") - r, num(e, "cy") - r, r * 2, r * 2);
            }
            case "ellipse": {
                double rx = num(e, "rx");
                double ry = num(e, "ry");
                return new Ellipse2D.Double(num(e, "cx") - rx, num(e, "cy") - ry, rx * 2, ry * 2);
            }
            case "rect": {
                double rx = e.hasAttribute("rx") ? num(e, "rx") : (e.hasAttribute("ry") ? num(e, "ry") : 0);
                double ry = e.hasAttribute("ry") ? num(e, "ry") : rx;
                return rx > 0 || ry > 0
                        ? new RoundRectangle2D.Double(num(e, "x"), num(e, "y"), num(e, "width"), num(e, "height"), rx * 2, ry * 2)
                        : new Rectangle2D.Double(num(e, "x"), num(e, "y"), num(e, "width"), num(e, "height"));
            }
            case "line":
                return new Line2D.Double(num(e, "x1"), num(e, "y1"), num(e, "x2"), num(e, "y2"));
            case "polyline":
            case "polygon": {
                PathScanner sc = new PathScanner(e.getAttribute("points"));
                Path2D.Double p = new Path2D.Double();
                boolean first = true;
                while (sc.hasNumber()) {
                    double px = sc.number();
                    double py = sc.number();
                    if (first) {
                        p.moveTo(px, py);
                        first = false;
                    } else {
                        p.lineTo(px, py);
                    }
                }
                if (e.getTagName().equals("polygon")) {
                    p.closePath();
                }
                return p;
            }
            default:
                return null;
        }
    }

    private static double num(org.w3c.dom.Element e, String attribute) {
        String v = e.getAttribute(attribute);
        return v.isEmpty() ? 0 : Double.parseDouble(v);
    }

    /** Minimal SVG path parser: M L H V C S Q T A Z, absolute and relative. */
    static Path2D.Double parsePath(String d) {
        Path2D.Double p = new Path2D.Double();
        PathScanner sc = new PathScanner(d);
        double cx = 0;
        double cy = 0;
        double sx = 0;
        double sy = 0;
        double lastCtrlX = 0;
        double lastCtrlY = 0;
        char cmd = 0;
        char prev = 0;
        while (sc.more()) {
            if (sc.atCommand()) {
                cmd = sc.command();
            } else if (cmd == 0) {
                break;
            }
            boolean rel = Character.isLowerCase(cmd);
            switch (Character.toUpperCase(cmd)) {
                case 'M': {
                    double x = sc.number() + (rel ? cx : 0);
                    double y = sc.number() + (rel ? cy : 0);
                    p.moveTo(x, y);
                    cx = x;
                    cy = y;
                    sx = x;
                    sy = y;
                    cmd = rel ? 'l' : 'L';
                    break;
                }
                case 'L': {
                    double x = sc.number() + (rel ? cx : 0);
                    double y = sc.number() + (rel ? cy : 0);
                    p.lineTo(x, y);
                    cx = x;
                    cy = y;
                    break;
                }
                case 'H': {
                    double x = sc.number() + (rel ? cx : 0);
                    p.lineTo(x, cy);
                    cx = x;
                    break;
                }
                case 'V': {
                    double y = sc.number() + (rel ? cy : 0);
                    p.lineTo(cx, y);
                    cy = y;
                    break;
                }
                case 'C': {
                    double x1 = sc.number() + (rel ? cx : 0);
                    double y1 = sc.number() + (rel ? cy : 0);
                    double x2 = sc.number() + (rel ? cx : 0);
                    double y2 = sc.number() + (rel ? cy : 0);
                    double x = sc.number() + (rel ? cx : 0);
                    double y = sc.number() + (rel ? cy : 0);
                    p.curveTo(x1, y1, x2, y2, x, y);
                    lastCtrlX = x2;
                    lastCtrlY = y2;
                    cx = x;
                    cy = y;
                    break;
                }
                case 'S': {
                    boolean smooth = prev == 'C' || prev == 'S';
                    double x1 = smooth ? 2 * cx - lastCtrlX : cx;
                    double y1 = smooth ? 2 * cy - lastCtrlY : cy;
                    double x2 = sc.number() + (rel ? cx : 0);
                    double y2 = sc.number() + (rel ? cy : 0);
                    double x = sc.number() + (rel ? cx : 0);
                    double y = sc.number() + (rel ? cy : 0);
                    p.curveTo(x1, y1, x2, y2, x, y);
                    lastCtrlX = x2;
                    lastCtrlY = y2;
                    cx = x;
                    cy = y;
                    break;
                }
                case 'Q': {
                    double x1 = sc.number() + (rel ? cx : 0);
                    double y1 = sc.number() + (rel ? cy : 0);
                    double x = sc.number() + (rel ? cx : 0);
                    double y = sc.number() + (rel ? cy : 0);
                    p.quadTo(x1, y1, x, y);
                    lastCtrlX = x1;
                    lastCtrlY = y1;
                    cx = x;
                    cy = y;
                    break;
                }
                case 'T': {
                    boolean smooth = prev == 'Q' || prev == 'T';
                    double x1 = smooth ? 2 * cx - lastCtrlX : cx;
                    double y1 = smooth ? 2 * cy - lastCtrlY : cy;
                    double x = sc.number() + (rel ? cx : 0);
                    double y = sc.number() + (rel ? cy : 0);
                    p.quadTo(x1, y1, x, y);
                    lastCtrlX = x1;
                    lastCtrlY = y1;
                    cx = x;
                    cy = y;
                    break;
                }
                case 'A': {
                    double rx = sc.number();
                    double ry = sc.number();
                    double rotation = sc.number();
                    boolean large = sc.flag();
                    boolean sweep = sc.flag();
                    double x = sc.number() + (rel ? cx : 0);
                    double y = sc.number() + (rel ? cy : 0);
                    arcTo(p, cx, cy, rx, ry, rotation, large, sweep, x, y);
                    cx = x;
                    cy = y;
                    break;
                }
                case 'Z': {
                    p.closePath();
                    cx = sx;
                    cy = sy;
                    break;
                }
                default:
                    return p;
            }
            prev = Character.toUpperCase(cmd);
            if (prev == 'M') {
                prev = 'L';
            }
        }
        return p;
    }

    /** Converts an SVG endpoint arc into cubic Bezier segments. */
    private static void arcTo(Path2D.Double p, double x1, double y1, double rx, double ry, double rotationDeg,
                              boolean large, boolean sweep, double x2, double y2) {
        if (x1 == x2 && y1 == y2) {
            return;
        }
        rx = Math.abs(rx);
        ry = Math.abs(ry);
        if (rx == 0 || ry == 0) {
            p.lineTo(x2, y2);
            return;
        }
        double phi = Math.toRadians(rotationDeg);
        double cos = Math.cos(phi);
        double sin = Math.sin(phi);
        double dx = (x1 - x2) / 2;
        double dy = (y1 - y2) / 2;
        double x1p = cos * dx + sin * dy;
        double y1p = -sin * dx + cos * dy;
        double lambda = x1p * x1p / (rx * rx) + y1p * y1p / (ry * ry);
        if (lambda > 1) {
            double scale = Math.sqrt(lambda);
            rx *= scale;
            ry *= scale;
        }
        double numerator = rx * rx * ry * ry - rx * rx * y1p * y1p - ry * ry * x1p * x1p;
        double denominator = rx * rx * y1p * y1p + ry * ry * x1p * x1p;
        double coef = Math.sqrt(Math.max(0, numerator / denominator));
        if (large == sweep) {
            coef = -coef;
        }
        double cxp = coef * rx * y1p / ry;
        double cyp = -coef * ry * x1p / rx;
        double ccx = cos * cxp - sin * cyp + (x1 + x2) / 2;
        double ccy = sin * cxp + cos * cyp + (y1 + y2) / 2;
        double theta = angle(1, 0, (x1p - cxp) / rx, (y1p - cyp) / ry);
        double delta = angle((x1p - cxp) / rx, (y1p - cyp) / ry, (-x1p - cxp) / rx, (-y1p - cyp) / ry);
        if (!sweep && delta > 0) {
            delta -= 2 * Math.PI;
        } else if (sweep && delta < 0) {
            delta += 2 * Math.PI;
        }
        int segments = (int) Math.ceil(Math.abs(delta) / (Math.PI / 2) - 1e-9);
        double step = delta / segments;
        double t = 4.0 / 3.0 * Math.tan(step / 4);
        for (int i = 0; i < segments; i++) {
            double a1 = theta + i * step;
            double a2 = a1 + step;
            double c1 = Math.cos(a1);
            double s1 = Math.sin(a1);
            double c2 = Math.cos(a2);
            double s2 = Math.sin(a2);
            double[] e1 = {c1 - t * s1, s1 + t * c1};
            double[] e2 = {c2 + t * s2, s2 - t * c2};
            double[] end = {c2, s2};
            p.curveTo(
                    cos * rx * e1[0] - sin * ry * e1[1] + ccx, sin * rx * e1[0] + cos * ry * e1[1] + ccy,
                    cos * rx * e2[0] - sin * ry * e2[1] + ccx, sin * rx * e2[0] + cos * ry * e2[1] + ccy,
                    cos * rx * end[0] - sin * ry * end[1] + ccx, sin * rx * end[0] + cos * ry * end[1] + ccy);
        }
    }

    private static double angle(double ux, double uy, double vx, double vy) {
        double sign = ux * vy - uy * vx < 0 ? -1 : 1;
        double dot = ux * vx + uy * vy;
        double len = Math.hypot(ux, uy) * Math.hypot(vx, vy);
        return sign * Math.acos(Math.max(-1, Math.min(1, dot / len)));
    }

    /** Tokenizer for SVG path data and point lists (handles compact forms like "1-.5" and "0 01"). */
    private static final class PathScanner {
        private final String s;
        private int i;

        PathScanner(String s) {
            this.s = s == null ? "" : s;
        }

        private void skipSeparators() {
            while (i < s.length() && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == ',')) {
                i++;
            }
        }

        boolean more() {
            skipSeparators();
            return i < s.length();
        }

        boolean atCommand() {
            skipSeparators();
            return i < s.length() && Character.isLetter(s.charAt(i));
        }

        char command() {
            skipSeparators();
            return s.charAt(i++);
        }

        boolean hasNumber() {
            skipSeparators();
            if (i >= s.length()) {
                return false;
            }
            char c = s.charAt(i);
            return c == '-' || c == '+' || c == '.' || Character.isDigit(c);
        }

        boolean flag() {
            skipSeparators();
            return s.charAt(i++) == '1';
        }

        double number() {
            skipSeparators();
            int start = i;
            if (i < s.length() && (s.charAt(i) == '-' || s.charAt(i) == '+')) {
                i++;
            }
            while (i < s.length() && Character.isDigit(s.charAt(i))) {
                i++;
            }
            if (i < s.length() && s.charAt(i) == '.') {
                i++;
                while (i < s.length() && Character.isDigit(s.charAt(i))) {
                    i++;
                }
            }
            if (i < s.length() && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
                i++;
                if (i < s.length() && (s.charAt(i) == '-' || s.charAt(i) == '+')) {
                    i++;
                }
                while (i < s.length() && Character.isDigit(s.charAt(i))) {
                    i++;
                }
            }
            return Double.parseDouble(s.substring(start, i));
        }
    }
}
