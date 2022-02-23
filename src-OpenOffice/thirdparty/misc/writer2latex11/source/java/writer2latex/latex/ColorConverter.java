/************************************************************************
 *
 *  ColorConverter.java
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 *  MA  02111-1307  USA
 *
 *  Copyright: 2002-2008 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.0 (2008-11-23)
 *
 */

package writer2latex.latex;

import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;
import writer2latex.office.OfficeReader;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.Misc;


/** This class converts color 
 */
public class ColorConverter extends ConverterHelper {

    private static final int RED = 0;
    private static final int GREEN = 1;
    private static final int BLUE = 2;
    
    private boolean bUseColor;

    /** <p>Constructs a new <code>CharStyleConverter</code>.</p>
     */
    public ColorConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
        super(ofr,config,palette);

        // We use color if requested in the configuration, however ignoring
        // all formatting overrides this
        bUseColor = config.useColor() && config.formatting()>LaTeXConfig.IGNORE_ALL;
    }

    public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
	    if (bUseColor) {
            pack.append("\\usepackage{color}").nl();
        }
    }
	
    public void setNormalColor(String sColor, LaTeXDocumentPortion ldp) {
        if (bUseColor && sColor!=null) {
            ldp.append("\\renewcommand\\normalcolor{\\color")
               .append(color(sColor)).append("}").nl();
        }
    }
	
    public void applyNormalColor(BeforeAfter ba) {
        if (bUseColor) { ba.add("\\normalcolor",""); }
    }
	
    /** <p>Apply foreground color.</p>
     *  @param style the OOo style to read attributesfrom
     *  @param bDecl true if declaration form is required
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     *  @param context the current context
     */
    public void applyColor(StyleWithProperties style, boolean bDecl, boolean bInherit, BeforeAfter ba, Context context) {
        // Note: if bDecl is true, nothing will be put in the "after" part of ba.
        if (bUseColor && style!=null) {
            String sColor = style.getProperty(XMLString.FO_COLOR,bInherit);
            if (sColor!=null) {
                if (!sColor.equals(context.getFontColor())) {
                    // Convert color if it differs from the current font color
                    context.setFontColor(sColor);
                    applyColor(sColor, bDecl, ba, context);
                }
            }
            else {
                // No color; maybe automatic color?
                String sAutomatic = style.getProperty(XMLString.STYLE_USE_WINDOW_FONT_COLOR,bInherit);
                if (sAutomatic==null && bInherit) {
                    // We may need to inherit this property from the default style
                    StyleWithProperties defaultStyle = ofr.getDefaultParStyle();
                    if (defaultStyle!=null) {
                        sAutomatic = defaultStyle.getProperty(XMLString.STYLE_USE_WINDOW_FONT_COLOR,bInherit);
                    }
                }
                if ("true".equals(sAutomatic)) {
                    // Automatic color based on background
                    if (context.getBgColor()!=null) { applyAutomaticColor(ba,bDecl,context); } 
                }
            }
        }
    }
	
    /** <p>Apply a specific foreground color.</p>
     *  @param sColor the rgb color to use
     *  @param bDecl true if declaration form is required
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    public void applyColor(String sColor, boolean bDecl, BeforeAfter ba, Context context) {
        // Note: if bDecl is true, nothing will be put in the "after" part of ba.
        if (bUseColor && sColor!=null) {
            // If there's a background color, allow all colors
            String s = context.getBgColor()!=null ? fullcolor(sColor) : color(sColor);
            if (s!=null) {
                if (bDecl) { ba.add("\\color"+s,""); }
                else { ba.add("\\textcolor"+s+"{","}"); }
            }
        }
    }
	
    public void applyBgColor(String sCommand, String sColor, BeforeAfter ba, Context context) {
        // Note: Will only fill "before" part of ba
        if (sColor!=null && !"transparent".equals(sColor)) {
            String s = fullcolor(sColor);
            if (bUseColor && s!=null) {
                context.setBgColor(sColor);
                ba.add(sCommand+s,"");
            }
        }
    }
	
    public void applyAutomaticColor(BeforeAfter ba, boolean bDecl, Context context) {
        String s = automaticcolor(context.getBgColor());
        if (s!=null) {
            if (bDecl) { ba.add("\\color"+s,""); }
            else { ba.add("\\textcolor"+s+"{","}"); }
        }
    }
	
    private static final String automaticcolor(String sBgColor) {
        if (sBgColor!=null && sBgColor.length()==7) {
            float[] rgb = getRgb(sBgColor);
            if (rgb[RED]+rgb[GREEN]+rgb[BLUE]<0.6) {
                // Dark background
                return "{white}";
            }
        }
        return "{black}";
    }
	
    private static final String color(String sColor){
        if ("#000000".equalsIgnoreCase(sColor)) { return "{black}"; }
        else if ("#ff0000".equalsIgnoreCase(sColor)) { return "{red}"; }
        else if ("#00ff00".equalsIgnoreCase(sColor)) { return "{green}"; }
        else if ("#0000ff".equalsIgnoreCase(sColor)) { return "{blue}"; }
        else if ("#ffff00".equalsIgnoreCase(sColor)) { return "{yellow}"; }
        else if ("#ff00ff".equalsIgnoreCase(sColor)) { return "{magenta}"; }
        else if ("#00ffff".equalsIgnoreCase(sColor)) { return "{cyan}"; }
        //no white, since we don't have background colors:
        //else if ("#ffffff".equalsIgnoreCase(sColor)) { return "{white}"; }
        else {
            if (sColor==null || sColor.length()!=7) return null;
            float[] rgb = getRgb(sColor);
            // avoid very bright colors (since we don't have background colors):
            if (rgb[RED]+rgb[GREEN]+rgb[BLUE]>2.7) { return "{black}"; }
            else { return "[rgb]{"+rgb[RED]+","+rgb[GREEN]+","+rgb[BLUE]+"}"; }
        }
    }
    
    private static final String fullcolor(String sColor){
        if ("#000000".equalsIgnoreCase(sColor)) { return "{black}"; }
        else if ("#ff0000".equalsIgnoreCase(sColor)) { return "{red}"; }
        else if ("#00ff00".equalsIgnoreCase(sColor)) { return "{green}"; }
        else if ("#0000ff".equalsIgnoreCase(sColor)) { return "{blue}"; }
        else if ("#ffff00".equalsIgnoreCase(sColor)) { return "{yellow}"; }
        else if ("#ff00ff".equalsIgnoreCase(sColor)) { return "{magenta}"; }
        else if ("#00ffff".equalsIgnoreCase(sColor)) { return "{cyan}"; }
        else if ("#ffffff".equalsIgnoreCase(sColor)) { return "{white}"; }
        else {
            // This could mean transparent:
            if (sColor==null || sColor.length()!=7) return null;
            float[] rgb = getRgb(sColor);
            return "[rgb]{"+rgb[RED]+","+rgb[GREEN]+","+rgb[BLUE]+"}";
        }
    }
	
    private static final float[] getRgb(String sColor) {
        float[] rgb = new float[3];
        rgb[RED] = (float)Misc.getIntegerFromHex(sColor.substring(1,3),0)/255;
        rgb[GREEN] = (float)Misc.getIntegerFromHex(sColor.substring(3,5),0)/255;
        rgb[BLUE] = (float)Misc.getIntegerFromHex(sColor.substring(5,7),0)/255;
        return rgb;
    }


}
