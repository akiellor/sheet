package org.github.akiellor.sheet;

import net.sf.ghost4j.document.DocumentException;
import net.sf.ghost4j.document.PDFDocument;
import org.javafunk.funk.Lazily;
import org.javafunk.funk.datastructures.IntegerRange;
import org.javafunk.funk.datastructures.tuples.Pair;
import org.javafunk.funk.functors.functions.UnaryFunction;
import org.javafunk.funk.monads.Option;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.javafunk.funk.Eagerly.first;
import static org.javafunk.funk.Literals.tuple;

public class PdfCursor implements Iterator<Pair<Option<Page>, Option<Page>>> {
    private final PDFDocument document;
    private Iterator<Pair<Option<Integer>, Option<Integer>>> pagePairs;

    @SuppressWarnings("unchecked")
    public PdfCursor(PDFDocument document){
        this.document = document;
        try {
            this.pagePairs = Lazily.map(Lazily.batch(new IntegerRange(0, document.getPageCount()), 2), toPair()).iterator();
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
    }

    private UnaryFunction<? super Iterable<Integer>, Pair<Option<Integer>, Option<Integer>>> toPair() {
        return new UnaryFunction<Iterable<Integer>, Pair<Option<Integer>, Option<Integer>>>() {
            @Override public Pair<Option<Integer>, Option<Integer>> call(Iterable<Integer> iterable) {
                return tuple(first(iterable), first(Lazily.drop(iterable, 1)));
            }
        };
    }

    @Override public boolean hasNext() {
        return pagePairs.hasNext();
    }

    @Override public Pair<Option<Page>, Option<Page>> next() {
        if(!hasNext()){
            throw new NoSuchElementException();
        }

        Pair<Option<Integer>, Option<Integer>> next = pagePairs.next();
        return tuple(next.getFirst().flatMap(toPage()), next.getSecond().flatMap(toPage()));
    }

    private UnaryFunction<? super Integer, ? extends Option<Page>> toPage() {
        return new UnaryFunction<Integer, Option<Page>>() {
            @Override public Option<Page> call(Integer firstArgument) {
                return Page.page(document, firstArgument);
            }
        };
    }

    @Override public void remove() {
        throw new UnsupportedOperationException();
    }
}
