package org.github.akiellor.sheet;

import net.sf.ghost4j.document.DocumentException;
import net.sf.ghost4j.document.PDFDocument;
import net.sf.ghost4j.renderer.SimpleRenderer;
import org.javafunk.funk.monads.Option;

import java.awt.image.BufferedImage;

public class Page {
    private final PDFDocument document;
    private final int page;

    public static Option<Page> page(PDFDocument document, int page){
        if(inRange(document, page)){
            return Option.some(new Page(document, page));
        }else{
            return Option.none();
        }
    }

    private Page(PDFDocument document, int page) {
        this.document = document;
        this.page = page;
    }

    public BufferedImage getImage() {
        SimpleRenderer renderer = new SimpleRenderer();

        renderer.setResolution(72);

        try {
            return (BufferedImage) renderer.render(document).get(page);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean inRange(PDFDocument document, int pageNumber) {
        try {
            return pageNumber >= 0 && pageNumber + 1 <= document.getPageCount();
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
    }
}
