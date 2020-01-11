package net.tiny.dao;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;


public class PageTest {

    @Test
    public void testGetSegment() throws Exception {
        List<Integer> segment;
        segment = createPage(String.class, 1, 57L, 10).getSegment();
        assertEquals(4, segment.size());
        segment = createPage(String.class, 2, 57L, 10).getSegment();
        assertEquals(4, segment.size());
        segment = createPage(String.class, 3, 57L, 10).getSegment();
        assertEquals(4, segment.size());
        segment = createPage(String.class, 4, 57L, 10).getSegment();
        assertEquals(4, segment.size());
        segment = createPage(String.class, 5, 57L, 10).getSegment();
        assertEquals(4, segment.size());
        segment = createPage(String.class, 6, 57L, 10).getSegment();
        assertEquals(4, segment.size());

        segment = createPage(String.class, 1, 77L, 10).getSegment();
        assertEquals(5, segment.size());
        segment = createPage(String.class, 3, 77L, 10).getSegment();
        assertEquals(5, segment.size());
        segment = createPage(String.class, 4, 77L, 10).getSegment();
        assertEquals(5, segment.size());
        segment = createPage(String.class, 5, 77L, 10).getSegment();
        assertEquals(5, segment.size());
        segment = createPage(String.class, 6, 77L, 10).getSegment();
        assertEquals(5, segment.size());
        segment = createPage(String.class, 7, 77L, 10).getSegment();
        assertEquals(5, segment.size());
        segment = createPage(String.class, 8, 77L, 10).getSegment();
        assertEquals(5, segment.size());
        for(Integer num : segment) {
            System.out.println("segment: " + num);
        }
    }

    @Test
    public void testLastSegment() throws Exception {
        List<Integer> segment;
        segment = createPage(String.class, 6, 77L, 10).getSegment();
        assertEquals(5, segment.size());

        for(Integer num : segment) {
            System.out.println("segment: " + num);
        }
    }

    private <T> Page<T> createPage(Class<T> type, int pageNumber, long total, int pageSize) {
        Pageable pageable = new Pageable(pageNumber, pageSize);
        pageable.setSearchProperty("Title");
        pageable.setOrderProperty("date");
        pageable.setOrderDirection(Order.Direction.asc);
        List<T> content = new ArrayList<>();
        Page<T> page = new Page<>(content, total, pageable);
        return page;
    }

}
