// Central mock dataset mirroring PRD §21 schema.
// In production this is replaced by Java Servlet REST endpoints (§23).
// Shape intentionally matches what the backend should return (no is_correct leaked).

export const SEED_USERS = [
  { id: 1, name: "Admin User", email: "admin@ezexam.com", password: "admin123", role: "admin" },
  { id: 2, name: "Aarav Sharma", email: "student@ezexam.com", password: "student123", role: "student" },
];

export const SEED_EXAMS = [
  {
    id: 1,
    title: "Data Structures Mid-Term",
    description: "Stacks, queues, trees, graphs and time-complexity basics.",
    duration_minutes: 30,
    total_marks: 20,
    passing_percentage: 40,
    negative_mark: 0.25,
    status: "published",
    start_time: "2026-09-01T09:00",
    end_time: "2026-12-31T23:59",
    created_by: 1,
  },
  {
    id: 2,
    title: "Java Programming Quiz",
    description: "OOP concepts, collections, exception handling and JVM basics.",
    duration_minutes: 20,
    total_marks: 10,
    passing_percentage: 40,
    negative_mark: 0,
    status: "published",
    start_time: "2026-09-15T09:00",
    end_time: "2026-12-31T23:59",
    created_by: 1,
  },
  {
    id: 3,
    title: "DBMS Fundamentals",
    description: "ER models, normalization, SQL queries and transactions. Draft — not visible to students.",
    duration_minutes: 45,
    total_marks: 15,
    passing_percentage: 50,
    negative_mark: 0.5,
    status: "draft",
    start_time: "2026-10-10T09:00",
    end_time: "2026-12-31T23:59",
    created_by: 1,
  },
];

// questions: options carry is_correct internally, but api.js strips it for students.
export const SEED_QUESTIONS = [
  {
    id: 101, exam_id: 1, marks: 2, negative_marks: 0.25,
    question_text: "Which data structure follows FIFO?",
    options: [
      { id: 1001, text: "Stack", is_correct: false },
      { id: 1002, text: "Queue", is_correct: true },
      { id: 1003, text: "Tree", is_correct: false },
      { id: 1004, text: "Graph", is_correct: false },
    ],
  },
  {
    id: 102, exam_id: 1, marks: 2, negative_marks: 0.25,
    question_text: "What is the time complexity of binary search?",
    options: [
      { id: 1005, text: "O(n)", is_correct: false },
      { id: 1006, text: "O(log n)", is_correct: true },
      { id: 1007, text: "O(n²)", is_correct: false },
      { id: 1008, text: "O(1)", is_correct: false },
    ],
  },
  {
    id: 103, exam_id: 1, marks: 2, negative_marks: 0.25,
    question_text: "Which structure uses LIFO?",
    options: [
      { id: 1009, text: "Queue", is_correct: false },
      { id: 1010, text: "Stack", is_correct: true },
      { id: 1011, text: "Heap", is_correct: false },
      { id: 1012, text: "Linked List", is_correct: false },
    ],
  },
  {
    id: 104, exam_id: 1, marks: 2, negative_marks: 0.25,
    question_text: "In a BST, inorder traversal yields nodes in…",
    options: [
      { id: 1013, text: "Random order", is_correct: false },
      { id: 1014, text: "Sorted order", is_correct: true },
      { id: 1015, text: "Reverse order", is_correct: false },
      { id: 1016, text: "Level order", is_correct: false },
    ],
  },
  {
    id: 105, exam_id: 1, marks: 2, negative_marks: 0.25,
    question_text: "Which traversal is used for level-order?",
    options: [
      { id: 1017, text: "DFS", is_correct: false },
      { id: 1018, text: "BFS", is_correct: true },
      { id: 1019, text: "Inorder", is_correct: false },
      { id: 1020, text: "Postorder", is_correct: false },
    ],
  },
  {
    id: 201, exam_id: 2, marks: 2, negative_marks: 0,
    question_text: "Which keyword is used to inherit a class in Java?",
    options: [
      { id: 2001, text: "implements", is_correct: false },
      { id: 2002, text: "extends", is_correct: true },
      { id: 2003, text: "inherits", is_correct: false },
      { id: 2004, text: "super", is_correct: false },
    ],
  },
  {
    id: 202, exam_id: 2, marks: 2, negative_marks: 0,
    question_text: "Which collection does not allow duplicates?",
    options: [
      { id: 2005, text: "ArrayList", is_correct: false },
      { id: 2006, text: "LinkedList", is_correct: false },
      { id: 2007, text: "HashSet", is_correct: true },
      { id: 2008, text: "Vector", is_correct: false },
    ],
  },
  {
    id: 203, exam_id: 2, marks: 2, negative_marks: 0,
    question_text: "What is the default value of an int field?",
    options: [
      { id: 2009, text: "null", is_correct: false },
      { id: 2010, text: "0", is_correct: true },
      { id: 2011, text: "undefined", is_correct: false },
      { id: 2012, text: "1", is_correct: false },
    ],
  },
];

export const SEED_ATTEMPTS = [
  {
    id: 9001, user_id: 2, exam_id: 2, status: "submitted",
    start_time: "2026-09-28T10:00:00", submitted_at: "2026-09-28T10:12:00",
    score: 4, total: 6, correct: 2, wrong: 1, unanswered: 0,
    percentage: 66.67, passed: true,
  },
];
