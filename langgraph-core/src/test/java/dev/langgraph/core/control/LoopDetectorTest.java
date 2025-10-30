package dev.langgraph.core.control;

import dev.langgraph.core.NodeId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoopDetectorTest {

    @Test
    void shouldRecordVisits() {
        LoopDetector detector = new LoopDetector(10);
        NodeId nodeId = NodeId.of("node1");

        detector.recordVisit(nodeId);
        assertEquals(1, detector.getVisitCount(nodeId));

        detector.recordVisit(nodeId);
        assertEquals(2, detector.getVisitCount(nodeId));
    }

    @Test
    void shouldThrowOnExceedingLimit() {
        LoopDetector detector = new LoopDetector(3);
        NodeId nodeId = NodeId.of("node1");

        detector.recordVisit(nodeId);
        detector.recordVisit(nodeId);
        detector.recordVisit(nodeId);

        assertThrows(LoopDetector.InfiniteLoopException.class, () -> {
            detector.recordVisit(nodeId);
        });
    }

    @Test
    void shouldTrackMultipleNodes() {
        LoopDetector detector = new LoopDetector(5);
        NodeId node1 = NodeId.of("node1");
        NodeId node2 = NodeId.of("node2");

        detector.recordVisit(node1);
        detector.recordVisit(node2);
        detector.recordVisit(node1);

        assertEquals(2, detector.getVisitCount(node1));
        assertEquals(1, detector.getVisitCount(node2));
    }

    @Test
    void shouldReset() {
        LoopDetector detector = new LoopDetector(10);
        NodeId nodeId = NodeId.of("node1");

        detector.recordVisit(nodeId);
        detector.recordVisit(nodeId);
        assertEquals(2, detector.getVisitCount(nodeId));

        detector.reset();
        assertEquals(0, detector.getVisitCount(nodeId));
    }

    @Test
    void shouldReturnZeroForUnvisitedNode() {
        LoopDetector detector = new LoopDetector(10);
        NodeId nodeId = NodeId.of("unvisited");

        assertEquals(0, detector.getVisitCount(nodeId));
    }

    @Test
    void shouldThrowOnInvalidMaxIterations() {
        assertThrows(IllegalArgumentException.class, () -> {
            new LoopDetector(0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new LoopDetector(-1);
        });
    }

    @Test
    void shouldIncludeDetailsInException() {
        LoopDetector detector = new LoopDetector(2);
        NodeId nodeId = NodeId.of("looping-node");

        detector.recordVisit(nodeId);
        detector.recordVisit(nodeId);

        try {
            detector.recordVisit(nodeId);
            fail("Expected InfiniteLoopException");
        } catch (LoopDetector.InfiniteLoopException e) {
            assertTrue(e.getMessage().contains("looping-node"));
            assertTrue(e.getMessage().contains("3"));
            assertTrue(e.getMessage().contains("2"));
        }
    }
}
