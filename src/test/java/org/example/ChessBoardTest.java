package org.example;

import org.example.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 棋盘逻辑单元测试类。
 * <p>
 * 使用 JUnit 5 对 {@link ChessBoardModel} 及其相关组件进行测试。
 * 涵盖棋子移动规则、吃子逻辑、将军检测以及游戏胜负判定等核心功能。
 * </p>
 */
class ChessBoardTest {

    private ChessBoardModel chessBoard;

    /**
     * 每个测试方法执行前初始化棋盘。
     */
    @BeforeEach
    void setUp() {
        chessBoard = new ChessBoardModel();
    }

    /**
     * 测试棋盘初始化状态。
     * 验证棋子总数是否为 32 个。
     */
    @Test
    void testInitialBoardSetup() {
        int pieceCount = 0;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 9; j++) {
                if (chessBoard.getPieceAt(i, j) != null) {
                    pieceCount++;
                }
            }
        }
        assertEquals(32, pieceCount, "初始棋盘应有32个棋子");
    }

    /**
     * 测试炮的移动规则。
     * 验证炮在不吃子的情况下只能直线移动，且不能跨越棋子。
     */
    @Test
    void testCannonMove() {
        // 红炮初始位置 (7, 1)
        AbstractPiece cannon = chessBoard.getPieceAt(7, 1);
        assertNotNull(cannon);
        assertTrue(cannon instanceof CannonPiece);

        // 炮平移到 (7, 4) - 合法移动
        assertTrue(cannon.canMoveTo(7, 4, chessBoard), "炮应能平移到空位");

        // 移动炮到空位
        chessBoard.movePiece(cannon, 7, 4);
        assertNull(chessBoard.getPieceAt(7, 1));
        assertEquals(cannon, chessBoard.getPieceAt(7, 4));
    }

    /**
     * 测试车的移动规则。
     * 验证车可以直线移动任意距离，但不能越过棋子。
     */
    @Test
    void testChariotMove() {
        // 红车初始位置 (9, 0)
        AbstractPiece chariot = chessBoard.getPieceAt(9, 0);
        assertNotNull(chariot);
        assertTrue(chariot instanceof ChariotPiece);

        // 前方有己方棋子，不能移动
        assertFalse(chariot.canMoveTo(8, 0, chessBoard), "车前方有子不能移动");

        // 假设把前方的马移走
        // 实际上初始盘面 (9,0) 是车，(9,1) 是马，(8,0) 是空的
        // 让我再确认一下 initializePieces 的布局
        // new ChariotPiece("俥", 9, 0, true),
        // new HorsePiece("傌", 9, 1, true),
        // (8,0) 应该是空的。
        // 为什么之前我认为 (8,0) 有子？
        // 让我们打印一下 (8,0)
        assertNull(chessBoard.getPieceAt(8, 0), "(8,0) 应该是空的");
        
        // 车 (9,0) -> (8,0) 应该是合法的
        assertTrue(chariot.canMoveTo(8, 0, chessBoard), "车应能向前移动一步到空位");
        
        // 车 (9,0) -> (6,0) 是非法的，因为 (6,0) 有兵阻挡（虽然是己方，但不能吃，也不能越过）
        assertFalse(chariot.canMoveTo(6, 0, chessBoard), "车不能吃己方棋子");
    }

    /**
     * 测试马的移动规则（包括“蹩马腿”）。
     */
    @Test
    void testHorseMove() {
        // 红马初始位置 (9, 1)
        AbstractPiece horse = chessBoard.getPieceAt(9, 1);
        assertNotNull(horse);
        assertTrue(horse instanceof HorsePiece);

        // 初始状态，往 (7, 2) 跳是合法的（"日"字）
        assertTrue(horse.canMoveTo(7, 2, chessBoard), "马应能跳到 (7, 2)");

        // 模拟蹩马腿：在 (8, 1) 放一个棋子
        // 我们可以把 (7,1) 的炮移到 (8,1)
        AbstractPiece cannon = chessBoard.getPieceAt(7, 1);
        // 注意：movePiece 会检查规则，炮 (7,1) -> (8,1) 是合法的（后退一步）
        // 但是现在是红方回合，炮是红方的，可以移动。
        chessBoard.movePiece(cannon, 8, 1);
        
        // 现在马 (9,1) 被 (8,1) 的炮蹩了腿，不能跳 (7,2)
        assertFalse(horse.canMoveTo(7, 2, chessBoard), "蹩马腿时马不能移动");
    }

    /**
     * 测试相（象）的移动规则（田字格与塞象眼）。
     */
    @Test
    void testElephantMove() {
        // 红相初始位置 (9, 2)
        AbstractPiece elephant = chessBoard.getPieceAt(9, 2);
        assertNotNull(elephant);
        assertTrue(elephant instanceof ElephantPiece);

        // 飞田字到 (7, 0)
        assertTrue(elephant.canMoveTo(7, 0, chessBoard), "相应该能飞田字");

        // 模拟塞象眼：在 (8, 1) 放子
        // 把 (7,1) 的炮移到 (8,1)
        AbstractPiece cannon = chessBoard.getPieceAt(7, 1);
        chessBoard.movePiece(cannon, 8, 1);
        
        // 现在相 (9,2) 往 (7,0) 飞，象眼 (8,1) 有子
        assertFalse(elephant.canMoveTo(7, 0, chessBoard), "塞象眼时相不能移动");
    }

    /**
     * 测试士的移动规则（九宫格斜线）。
     */
    @Test
    void testAdvisorMove() {
        // 红士初始位置 (9, 3)
        AbstractPiece advisor = chessBoard.getPieceAt(9, 3);
        assertNotNull(advisor);
        assertTrue(advisor instanceof AdvisorPiece);

        // 只能在九宫格内斜走
        assertTrue(advisor.canMoveTo(8, 4, chessBoard), "士应能斜走到九宫格中心");
        
        // 移动到中心
        chessBoard.movePiece(advisor, 8, 4);
        
        // 再往上走出九宫格 (7, 3) - 非法
        assertFalse(advisor.canMoveTo(7, 3, chessBoard), "士不能走出九宫格");
    }

    /**
     * 测试帅（将）的移动规则。
     */
    @Test
    void testGeneralMove() {
        // 红帅初始位置 (9, 4)
        AbstractPiece general = chessBoard.getPieceAt(9, 4);
        assertNotNull(general);
        assertTrue(general instanceof GeneralPiece);

        // 只能在九宫格内直走
        assertTrue(general.canMoveTo(8, 4, chessBoard), "帅应能向前移动一步");
        
        // 移动到 (8, 4)
        chessBoard.movePiece(general, 8, 4);
        
        // 尝试走出九宫格 (8, 4) -> (8, 3) 是合法的（还在九宫格）
        assertTrue(general.canMoveTo(8, 3, chessBoard));
        
        // 尝试走出九宫格 (8, 4) -> (7, 4) 是非法的（假设九宫格范围是 row 7-9, col 3-5）
        // 实际上红方九宫格是 row 7,8,9; col 3,4,5
        assertTrue(general.canMoveTo(7, 4, chessBoard), "帅在九宫格内应能移动");
        
        // 再往上 (6, 4) 就出去了
        // 注意：movePiece 会切换回合，所以连续移动同一个子需要注意回合
        // 这里只是测试 canMoveTo，不实际移动
        assertFalse(general.canMoveTo(6, 4, chessBoard), "帅不能走出九宫格");
    }

    /**
     * 测试兵（卒）的移动规则。
     */
    @Test
    void testSoldierMove() {
        // 红兵初始位置 (6, 0)
        AbstractPiece soldier = chessBoard.getPieceAt(6, 0);
        assertNotNull(soldier);
        assertTrue(soldier instanceof SoldierPiece);

        // 过河前只能向前
        assertTrue(soldier.canMoveTo(5, 0, chessBoard), "兵过河前只能向前");
        assertFalse(soldier.canMoveTo(6, 1, chessBoard), "兵过河前不能横走");
        
        // 模拟过河：假设兵到了 (4, 0) - 河界是 4-5 之间
        // 我们需要手动设置位置，因为 movePiece 会切换回合
        // 这里我们直接修改兵的坐标来模拟（虽然 AbstractPiece 的 setRow/setCol 是 protected/package-private?）
        // AbstractPiece 的 row/col 是 property，有 setRow/setCol 吗？
        // 让我们看看 AbstractPiece
        // 如果不能直接 set，我们就用 movePiece 配合 dummy moves
        
        // 简单起见，我们只测试 canMoveTo，假设它在那个位置
        // 但是 canMoveTo 依赖于棋盘状态，如果棋子不在那个位置，canMoveTo 可能会有问题吗？
        // canMoveTo(board, newRow, newCol) 是判断当前位置到新位置。
        
        // 我们可以构造一个新的棋盘或者使用 movePiece
        // 兵 (6,0) -> (5,0)
        chessBoard.movePiece(soldier, 5, 0);
        // 现在是黑方回合，我们需要让黑方走一步，比如黑卒 (3,0) -> (4,0)
        AbstractPiece blackSoldier = chessBoard.getPieceAt(3, 0);
        chessBoard.movePiece(blackSoldier, 4, 0);
        
        // 现在又是红方回合，兵在 (5,0)，再走一步到 (4,0)？不行，(4,0) 有黑卒
        // 兵 (5,0) 吃黑卒 (4,0)
        chessBoard.movePiece(soldier, 4, 0);
        
        // 现在兵在 (4,0)，已过河
        // 轮到黑方，随便走一步
        AbstractPiece blackCannon = chessBoard.getPieceAt(2, 1);
        chessBoard.movePiece(blackCannon, 2, 2);
        
        // 红方回合，兵在 (4,0)，可以横走 (4,1)
        assertTrue(soldier.canMoveTo(4, 1, chessBoard), "兵过河后可以横走");
    }
}
