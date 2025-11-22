package org.example.model;

/**
 * 遗留的门面类，用于保持向后兼容性。
 * 实际逻辑委托给 {@link ChessBoardModel}。
 */
@Deprecated(forRemoval = true)
public final class ChessBoard extends ChessBoardModel {
}
