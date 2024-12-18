package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LibraryManagerTest {
  private NotificationService notificationService;
  private  UserService userService;
  private  LibraryManager libraryManager;

  @BeforeEach
  void setUp(){
    notificationService = mock(NotificationService.class);
    userService = mock(UserService.class);
    libraryManager = new LibraryManager(notificationService, userService);
  }

  @ParameterizedTest
  @CsvSource({
      "book1, 11",
      "book2, 1",
      "book3, 5",
  })
  public void testAddBookParameterized(String bookId, int quantity) {
    libraryManager.addBook(bookId, quantity);
    assertEquals(quantity, libraryManager.getAvailableCopies(bookId));

  }
  @Test
  void testBorrowBookIfUserNotActive(){
    when(userService.isUserActive("user1")).thenReturn(false);
    assertFalse(libraryManager.borrowBook("book1", "user1"));
    verify(notificationService).notifyUser("user1", "Your account is not active.");
  }

  @Test
  void testBorrowBookNotAvailable(){
    when(userService.isUserActive("user1")).thenReturn(true);
    libraryManager.addBook("book1", 0);
    assertFalse(libraryManager.borrowBook("book1", "user1"));
  }

  @Test
  void testBorrowBookSuccessful(){
    libraryManager.addBook("book1", 1);
    when(userService.isUserActive("user1")).thenReturn(true);

    assertTrue(libraryManager.borrowBook("book1", "user1"));
    assertEquals(0, libraryManager.getAvailableCopies("book1"));
    verify(notificationService).notifyUser(eq("user1"), contains("You have borrowed the book: " + "book1"));
  }

  @Test
  void testReturnBookNotBorrowedByUser() {
    libraryManager.addBook("book1", 1);
    assertFalse(libraryManager.returnBook("book1", "user2"));
  }

  @Test
  void testReturnBook() {
    when(userService.isUserActive("user1")).thenReturn(true);
    libraryManager.addBook("book1", 1);
    libraryManager.borrowBook("book1", "user1");
    assertTrue(libraryManager.returnBook("book1", "user1"));
    assertEquals(1, libraryManager.getAvailableCopies("book1"));
    verify(notificationService).notifyUser(eq("user1"), contains("You have returned the book: book1"));
  }

  @Test
  void testReturnBookByNotActiveUser() {
    when(userService.isUserActive("user1")).thenReturn(false);
    libraryManager.addBook("book1", 1);
    libraryManager.borrowBook("book1", "user1");
    assertFalse(libraryManager.returnBook("book1", "user1"));
    verify(notificationService, never()).notifyUser(eq("user1"), contains("You have returned the book: book1"));
  }

  @Test
  public void testReturnBook_BookNotBorrowedByUser() {
    libraryManager.borrowBook("book1", "user1");
    assertFalse(libraryManager.returnBook("book1", "user2"));
    assertEquals(0, libraryManager.getAvailableCopies("book1"));
    verify(notificationService, never()).notifyUser(eq("user2"), contains("You have returned the book: book1")); //проверка что оповещение не придет ни тому ни другому
    verify(notificationService, never()).notifyUser(eq("user1"), contains("You have returned the book: book1"));
  }

  @Test
  void testReturnBookNotBorrowedAtAll() {
    libraryManager.addBook("book1", 1);
    assertFalse(libraryManager.returnBook("book1", "user2"));
    verify(notificationService, never()).notifyUser(anyString(), anyString());
  }

  @Test
  void testCalculateDynamicLateFee_BestsellerAndPremiumMember() {
    double fee = libraryManager.calculateDynamicLateFee(5, true, true);
    assertEquals(3.00, fee, 0.01); // (5 days * 0.5 * 1.5 * 0.8)
  }

  @Test
  void testCalculateDynamicLateFee_BestsellerOnly() {
    double fee = libraryManager.calculateDynamicLateFee(5, true, false);
    assertEquals(3.75, fee, 0.01); // (5 days * 0.5 * 1.5)
  }

  @Test
  void testCalculateDynamicLateFee_PremiumMemberOnly() {
    double fee = libraryManager.calculateDynamicLateFee(5, false, true);
    assertEquals(2.00, fee, 0.01); // (5 days * 0.5 * 0.8)
  }

  @Test
  void testCalculateDynamicLateFee_Neither() {
    double fee = libraryManager.calculateDynamicLateFee(5, false, false);
    assertEquals(2.50, fee, 0.01); // (5 days * 0.5)
  }

  @Test
  void testCalculateDynamicLateFee_ZeroOverdueDays() {
    double fee = libraryManager.calculateDynamicLateFee(0, false, false);
    assertEquals(0.00, fee, 0.01);

  }

  @Test
  void testCalculateDynamicLateFee_NegativeOverdueDays() {
    assertThrows(IllegalArgumentException.class, () -> {
      libraryManager.calculateDynamicLateFee(-1, false, false);
    });

  }


}