package application.network.message;

public enum MessageType {
    // Authentication
    LOGIN, LOGOUT, SIGNUP,
    
    // Customer operations
    MAKE_RESERVATION, GET_AVAILABLE_TABLES,
    PLACE_ORDER, GET_MENU, GET_ORDER_STATUS,
    
    // Staff operations
    UPDATE_TABLE_STATUS, GET_ALL_RESERVATIONS,
    UPDATE_ORDER_STATUS, GET_ALL_ORDERS,
    
    // Common
    SUCCESS, ERROR, GET_UPDATES
}