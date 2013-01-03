package org.github.akiellor.sheet;

import net.sf.ghost4j.document.PDFDocument;
import org.javafunk.funk.Eagerly;
import org.javafunk.funk.Iterators;
import org.javafunk.funk.datastructures.tuples.Pair;
import org.javafunk.funk.functors.functions.BinaryFunction;
import org.javafunk.funk.functors.functions.UnaryFunction;
import org.javafunk.funk.monads.Option;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import static org.javafunk.funk.Literals.listFrom;

public class Main {
    private ListIterator<Pair<Option<Page>, Option<Page>>> cursor;
    private final JFrame frame;
    private Pair<Option<Page>, Option<Page>> currentPages;

    public static void main(String... args) throws Exception {
        new Main().go();
    }

    public Main() throws IOException{
        frame = new JFrame();
        frame.setVisible(true);
        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
    }

    private void go() throws Exception {
        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int exitCode = fc.showOpenDialog(frame);
        File file;
        if (exitCode == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
        } else {
            return;
        }

        List<File> pdfs = listFrom(file.listFiles(new FilenameFilter() {
            @Override public boolean accept(File file, String filename) {
                return filename.endsWith(".pdf");
            }
        }));

        cursor = Eagerly.reduce(pdfs, new LinkedList<Pair<Option<Page>, Option<Page>>>(), new BinaryFunction<LinkedList<Pair<Option<Page>, Option<Page>>>, File, LinkedList<Pair<Option<Page>, Option<Page>>>>() {
            @Override public LinkedList<Pair<Option<Page>, Option<Page>>> call(LinkedList<Pair<Option<Page>, Option<Page>>> firstArgument, File secondArgument) {
                PDFDocument document = new PDFDocument();
                try {
                    document.load(secondArgument);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                firstArgument.addAll(listFrom(Iterators.asIterable(new PdfCursor(document))));
                return firstArgument;
            }
        }).listIterator();

        currentPages = cursor.next();

        frame.addKeyListener(new KeyListener() {
            @Override public void keyTyped(KeyEvent keyEvent) {
            }

            @Override public synchronized void keyPressed(KeyEvent keyEvent) {
                System.out.println(keyEvent);
                if ((keyEvent.getKeyCode() == 37 || keyEvent.getKeyCode() == 109) && cursor.hasPrevious()) {
                    currentPages = cursor.previous();
                    render();
                }

                if ((keyEvent.getKeyCode() == 39 || keyEvent.getKeyCode() == 107) && cursor.hasNext()) {
                    currentPages = cursor.next();
                    render();
                }
            }

            @Override public void keyReleased(KeyEvent keyEvent) {
            }
        });

        render();
    }

    private void clear() {
        for (Component c : frame.getContentPane().getComponents()) {
            frame.getContentPane().remove(c);
        }
    }

    private void render() {
        UnaryFunction<Page, Image> transform = new UnaryFunction<Page, Image>() {
            @Override public Image call(Page image) {
                BufferedImage img = image.getImage();
                int type = (img.getTransparency() == Transparency.OPAQUE) ?
                        BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
                BufferedImage tmp = new BufferedImage(frame.getWidth() / 2, frame.getHeight(), type);
                Graphics2D g2 = tmp.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.drawImage(img, 0, 0, frame.getWidth() / 2, frame.getHeight(), null);
                g2.dispose();
                return tmp;
            }
        };

        clear();

        if (currentPages.getFirst().hasValue()) {
            JLabel firstLabel = new JLabel(new ImageIcon(currentPages.getFirst().map(transform).get()));
            frame.getContentPane().add(firstLabel, BorderLayout.WEST);
        }

        if (currentPages.getSecond().hasValue()) {
            JLabel secondLabel = new JLabel(new ImageIcon(currentPages.getSecond().map(transform).get()));
            frame.getContentPane().add(secondLabel, BorderLayout.EAST);
        }

        frame.setVisible(true);
        frame.getContentPane().repaint();
    }
}
