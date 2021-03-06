package com.magis.app.UI;

import com.magis.app.Configure;
import com.magis.app.Main;
import com.magis.app.home.StatsPage;
import com.magis.app.models.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class IntelligentTutor {

    private static StudentModel.Student student;
    private static LessonModel.ChapterModel chapter;
    private static QuizzesModel quizzesModel;
    private static TestsModel testsModel;
    private static int progress;
    private static ArrayList<Integer> visitedPages;

    private static TextFlow subText;
    private static ArrayList<Text> texts;
    private static Label recentActivityTitle;
    private static Label chapterTitleText;
    private static Text text;
    private static boolean hasTakenQuiz;
    private static boolean hasTakenTest;

    private static int newChapter;
    private static int newPage;

    /**
     * Generate a box that will dynamically suggest to the student where to pick up
     * NOTE: This method must only be called after the VBox exists on the home page and that recentPage and recentChapter != -1
     * @return a VBox filled with dynamically generated text
     */
    public static VBox generateRecentActivity() {
        student = Main.studentModel.getStudent();
        chapter = Main.lessonModel.getChapter(student.getRecentChapter());
        quizzesModel = Main.quizzesModel;
        testsModel = Main.testsModel;

        newPage = student.getRecentPage();
        newChapter = student.getRecentChapter();

        /*
        determine if the recent page was a lesson page (not a "begin quiz/test" page)
         */
        boolean onLessonPage = student.getRecentPage() < chapter.getNumPages();
        /*
        Check of the page has a title
        If so, we might use it later
         */
        boolean hasPageTitle = onLessonPage && chapter.getPage(student.getRecentPage()).getTitle() != null;
        /*
        Determine if we are on a "begin quiz" page
        First check if the chapter has a quiz
        Then check of the page index matches the index of the quiz page for that lesson
         */
        boolean onQuizPage = quizzesModel.hasQuiz(chapter.getTitle()) && student.getRecentPage() == chapter.getNumPages();
        /*
        Determine if we are on a "begin test" page
        First check if the chapter has a test
        Then check of the page index matches the index of the test page for that lesson
        Note: We add 1 if we have a quiz page, because that pushes the index of the test page up one more
         */
        int testPageIndex = ((quizzesModel.hasQuiz(chapter.getTitle()) ? 1 : 0) + chapter.getNumPages());
        boolean onTestPage = testsModel.hasTest(chapter.getTitle()) && student.getRecentPage() == testPageIndex;

        //if there exists a quiz
        if (quizzesModel.hasQuiz(chapter.getTitle())) {
            hasTakenQuiz = student.hasTakenQuiz(student.getRecentChapter());
        } else {
            //if it doesn't exist
            hasTakenQuiz = true; //default to true
        }
        //if there exists a test
        if (testsModel.hasTest(chapter.getTitle())) {
            hasTakenTest = student.hasTakenTest(student.getRecentChapter());
        } else {
            //if it doesn't exist
            hasTakenTest = true; //default to true
        }

        progress = student.getChapter(student.getRecentChapter()).getProgress();
        visitedPages = student.getChapter(student.getRecentChapter()).getPageVisited();

        /*
        Build the box
         */
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);

        //Title
        recentActivityTitle = new Label();
        recentActivityTitle.getStyleClass().add("box-title");
        recentActivityTitle.setTextAlignment(TextAlignment.LEFT);
        recentActivityTitle.setWrapText(true);

        //Chapter Title
        chapterTitleText = new Label("Chapter " + (student.getRecentChapter() + 1) + " - " + chapter.getTitle());
        chapterTitleText.setPadding(new Insets(25,0,0,0));
        chapterTitleText.getStyleClass().add("box-description");
        chapterTitleText.setStyle("-fx-font-family: \"Roboto Mono Bold\"; -fx-font-size: 11px");

        //Text description
        subText = new TextFlow();
        subText.setPadding(new Insets(25,0,0,0));
        subText.setTextAlignment(TextAlignment.LEFT);

        texts = new ArrayList<>();

        /*
        Populate the box
         */
        //if they are completely done with this chapter
        if (hasTakenQuiz && hasTakenTest) {
            if (progress == 100) recentActivityTitle.setText("Onward!");
            else recentActivityTitle.setText("Ready to move on?");

            if (progress == 100) addText("You've completely finished this chapter! ");
            else {
                addText("You've finished ");
                if (quizzesModel.hasQuiz(chapter.getTitle())) {
                    addText("your quiz ");
                }
                if (testsModel.hasTest(chapter.getTitle())) {
                    if (quizzesModel.hasQuiz(chapter.getTitle())) {
                        addText("and ");
                    }
                   addText("your test ");
                }
                addText("for this chapter! ");
            }

            //only suggest the user to continue learning Java from Magis if we're not 100% done with everything ever
            if (StatsPage.calculateOverallProgress() < 100) {
                /*
                Find the next chapter to suggest the user to pick up with
                Find the first page of the earliest chapter that has not been read
                 */
                ArrayList<StudentModel.Student.ChapterModel> chapters = student.getChapters();
                for (int chapter = 0; chapter < chapters.size(); chapter++) {
                    StudentModel.Student.ChapterModel chapterModel = chapters.get(chapter);
                    if (chapterModel.getProgress() < 100) {
                        ArrayList<Integer> pagesVisited = chapterModel.getPageVisited();
                        for (int page = 0; page < pagesVisited.size(); page++) {
                            if (pagesVisited.get(page) == 0) {
                                newPage = page;
                                newChapter = chapter;
                                break;
                            }
                        }
                        //if there's an unfinished chapter that is earlier than the current chapter
                        if (newChapter < student.getRecentChapter()) {
                            if (student.getChapter(newChapter).getProgress() == 0) {
                                addText("You haven't done ");
                            } else {
                                addText("You haven't finished ");
                            }

                            addTextBold("Chapter " + (newChapter + 1) + ": " + Main.lessonModel.getChapter(newChapter).getTitle());
                            addText(" yet. Want to do it now?");
                        } else { //it's an unfinished chapter after their current chapter
                            addText("Ready for the next chapter? If so, click to go to ");
                            addTextBold("Chapter " + (newChapter + 1) + ": " + Main.lessonModel.getChapter(newChapter).getTitle());

                            //if the student already made progress in the next chapter, then indicate which page they should jump to
                            if (student.getChapter(newChapter).getProgress() > 0) {
                                ArrayList<Integer> pagesVisited2 = student.getChapter(newChapter).getPageVisited();
                                for (int page = 0; page < pagesVisited2.size(); page++) {
                                    if (pagesVisited2.get(page) == 0) {
                                        addText(" on ");
                                        String title = Main.lessonModel.getChapter(newChapter).getPage(newPage).getTitle();
                                        if (title != null) {
                                            addTextBold("page " + (page + 1) + ": " + title);
                                        } else {
                                            addTextBold("page " + (page + 1));
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    } else if (Main.quizzesModel.hasQuiz(Main.lessonModel.getChapter(chapter).getTitle()) && !student.hasTakenQuiz(chapter)) {
                        newPage = Main.lessonModel.getChapter(chapter).getNumPages();
                        newChapter = chapter;

                        //if there's an unfinished chapter that is earlier than the current chapter
                        addText("You haven't taken the quiz for ");
                        addTextBold("Chapter " + (newChapter + 1) + ": " + Main.lessonModel.getChapter(newChapter).getTitle());
                        addText(" yet. Want to do it now?");
                        break;
                    } else if (Main.testsModel.hasTest(Main.lessonModel.getChapter(chapter).getTitle()) && !student.hasTakenTest(chapter)) {
                        newPage = Main.lessonModel.getChapter(chapter).getNumPages() + (quizzesModel.hasQuiz(Main.lessonModel.getChapter(chapter).getTitle()) ? 1 : 0);
                        newChapter = chapter;

                        //if there's an unfinished chapter that is earlier than the current chapter
                        addText("You haven't taken the test for ");
                        addTextBold("Chapter " + (newChapter + 1) + ": " + Main.lessonModel.getChapter(newChapter).getTitle());
                        addText(" yet. Want to do it now?");
                        break;
                    }
                }
            }
            chapterTitleText.setText("Chapter " + (student.getRecentChapter() + 1) + " - " + chapter.getTitle());
        } else if (testsModel.hasTest(chapter.getTitle()) && hasTakenTest && !hasTakenQuiz && quizzesModel.hasQuiz(chapter.getTitle())) {
            //if on the quizzes page (edge case)
            if (student.getRecentPage() == chapter.getNumPages()) {
                recentActivityTitle.setText("Take your quiz real quick?");
            }
            //probably on the test page or trying to advance to another chapter
            else {
                recentActivityTitle.setText("Hold on a sec!");
            }
            newPage = chapter.getNumPages();
            addText("You haven't taken the quiz for ");
            addTextBold("Chapter " + (student.getRecentChapter() + 1) + ": " + Main.lessonModel.getChapter(student.getRecentChapter()).getTitle());
            addText(" yet, even though you already took the test. Want to do it now?");
        }
        else if ((onQuizPage && !hasTakenQuiz) || (progress == 100 && !hasTakenQuiz)) {
            if (checkForCompleteProgress("quiz")) {
                //this code will only execute if progress == 100
                newPage = chapter.getNumPages();
                recentActivityTitle.setText("Ready to take your quiz?");
                addText("You finished reading ");
                addTextBold("Chapter " + (newChapter + 1) + ": " + Main.lessonModel.getChapter(newChapter).getTitle());
                addText("! If you're ready to take your quiz, click here.");
            }
        } else if ((onTestPage && !hasTakenTest) || (progress == 100 && !hasTakenTest)) {
            //make sure the student has read everything first
            if (checkForCompleteProgress("test")) {
                //this code will only execute if progress == 100
                //make sure the student has taken the quiz (if there is one)
                if (!checkForIncompleteQuiz()) {
                    newPage = chapter.getNumPages() + (quizzesModel.hasQuiz(chapter.getTitle()) ? 1 : 0);
                    recentActivityTitle.setText("Ready to take your test?");
                    addText("You finished reading ");
                    addTextBold("Chapter " + (newChapter + 1) + ": " + Main.lessonModel.getChapter(newChapter).getTitle());
                    addText("! If you're ready to take your test, click here.");
                }
            }
        } else if (onLessonPage) {
            recentActivityTitle.setText("Pick up where you left off?");
            if (progress > 80) {
                addTextBold("You're almost done with this chapter! ");
            }
            addText("Click here to return to your next page on ");

            ArrayList<Integer> pagesVisited = student.getChapter(student.getRecentChapter()).getPageVisited();
            for (int page = 0; page < pagesVisited.size(); page++) {
                if (pagesVisited.get(page) == 0) {
                    newPage = page;
                    break;
                }
            }
            if (hasPageTitle) {
                addTextBold("Page " + (newPage + 1) + ": " + chapter.getPage(newPage).getTitle());
            } else {
                addTextBold("Page " + (newPage + 1));
            }
        }

        for (Text text : texts) {
            subText.getChildren().add(text);
        }
        box.getChildren().addAll(recentActivityTitle, chapterTitleText, subText);
        return box;
    }

    /**
     * Generate a box that will dynamically suggest to the student how to improve their Java abilities
     * @return a VBox filled with dynamically generated text
     */
    public static VBox generateInsights() {
        /*
        METHOD OF CALCULATION:
        The higher the value for the worstTestScore or worstQuizScore, the worse.
        We calculate them as such:
        Quiz: (1 - (score / 100)) * 2
        Test: (1 - (score / 100)) * 3

        Tests are weighted more heavily than quizzes.

        EXAMPLE:
        Quiz: (1 - (40 / 100)) * 2 = 1.2
        Test: (1 - (50 / 100)) * 3 = 1.5

        Therefore, even though the quiz score is worse, the test is weighted more heavily and thus
        is counted as "worse"
         */
        VBox insights = new VBox();
        insights.setMaxWidth(1000);
        insights.getStyleClass().addAll("chapter-box", "stats-box");
        texts = new ArrayList<>();
        subText = new TextFlow();
        subText.setTextAlignment(TextAlignment.CENTER);
        subText.setPadding(new Insets(25));
        double worstTestScore = 0;
        int worstTestIndex = -1; //holds which chapter number the worst test is associated with
        double worstQuizScore = 0;
        int worstQuizIndex = -1; //holds which chapter number the worst quiz is associated with
        student = Main.studentModel.getStudent();
        quizzesModel = Main.quizzesModel;
        testsModel = Main.testsModel;
        int numChapters = Main.lessonModel.getNumChapters();

        for (int i = 0; i < numChapters; i++) {
            /*
            Find which test they did the worst on (as long as it's below the minimum test score threshold defined in Configure.java)
             */
            if (student.hasTakenTest(i) && student.getTest(i).getBestScore() < Configure.MINIMUM_TEST_SCORE) {
                double score = student.getTest(i).getBestScore() / 100.0;
                if ((1 - score) * 3 > worstTestScore) {
                    worstTestScore = (1 - score) * 3;
                    worstTestIndex = i;
                }
            }
             /*
            Find which quiz they did the worst on (as long as it's below the minimum test score threshold defined in Configure.java)
             */
            if (student.hasTakenQuiz(i) && student.getQuiz(i).getBestScore() < Configure.MINIMUM_QUIZ_SCORE) {
                double score = student.getQuiz(i).getBestScore() / 100.0;
                if ((1 - score) * 2 > worstTestScore) {
                    worstQuizScore = (1 - score) * 2;
                    worstQuizIndex = i;
                }
            }
        }

        boolean hasTakenAnyExam = false;
        for (int i = 0; i < numChapters; i++) {
            if (student.hasTakenTest(i) || student.hasTakenQuiz(i)) {
                hasTakenAnyExam = true;
                break;
            }
        }
        if (!hasTakenAnyExam) {
            addText("Come back here when you've taken a quiz or a test for insights. Until then, go learn some Java!");
        } else if (worstQuizScore == 0 && worstTestScore == 0) {
            addTextBold("Nice job! You've been doing well on your quizzes and your tests so far. Keep up the great work!");
            int numChaptersNotDoneReading = 0;
            for (int i = 0; i < numChapters; i++) {
                String chapterTitle = Main.lessonModel.getChapter(i).getTitle();
                boolean hasTakenQuiz = quizzesModel.hasQuiz(chapterTitle) && student.hasTakenQuiz(i);
                boolean hasTakenTest = testsModel.hasTest(chapterTitle) && student.hasTakenTest(i);
                if (hasTakenQuiz && hasTakenTest && student.getChapter(i).getProgress() < 100) {
                    numChaptersNotDoneReading++;
                }
            }
            if (numChaptersNotDoneReading > 0) {
                addText("\n\nJust as a heads up, you finished your exams for ");
                for (int i = 0; i < numChapters; i++) {
                    String chapterTitle = Main.lessonModel.getChapter(i).getTitle();
                    boolean hasTakenQuiz = !quizzesModel.hasQuiz(chapterTitle) || student.hasTakenQuiz(i);
                    boolean hasTakenTest = !testsModel.hasTest(chapterTitle) || student.hasTakenTest(i);
                    if (hasTakenQuiz && hasTakenTest && student.getChapter(i).getProgress() < 100) {
                        addTextBold(" chapter " + (i + 1) + ": " + chapterTitle + ", ");
                        numChaptersNotDoneReading--;
                        if (numChaptersNotDoneReading == 1) {
                            addText("and");
                        }
                    }
                }
                addText(" but you never finished the reading for them. If you want the best mastery of the material, why don't you go back and finish those readings?" +
                        "\n\nYou can view your progress of reading on the homepage next to every chapter, or right below");
            }
        }
        /*
        There exists a quiz a student did very poorly on, even worse than a test despite tests being more heavily weighted
         */
        else if (worstQuizScore > worstTestScore) {
            LessonModel.ChapterModel chapter = Main.lessonModel.getChapter(worstQuizIndex);
            addText("It looks like your biggest struggle is with ");
            addTextColor("Chapter " + (worstQuizIndex + 1) + ": " + chapter.getTitle());
            addText(". You achieved a");
            addTextColor(" quiz score ");
            addText("of ");
            addTextRed(student.getQuiz(worstQuizIndex).getBestScore() + "%. ");
            int progress = student.getChapter(worstQuizIndex).getProgress();
            if (progress < 80) {
                addTextBold("A possible reason is that your reading progress is ");
                if (progress < 60) {
                    addTextBold("only ");
                }
                addTextColor(student.getChapter(worstQuizIndex).getProgress() + "%.");
                addText(" Make sure you read and understand the material before taking with your quiz. " +
                        "\nYou should probably go back and finish the material and then ");
                Random rand = new Random();
                //give slight variance to the wording
                if (rand.nextInt(2) == 0) {
                    addText("take another shot");
                } else {
                    addText("try again");
                }
                addText(" at the quiz.");
            } else {
                addText("\n\nIf you are still not comfortable with the material, it might be necessary to use additional " +
                        "resources or simply practice more. If you were to retake the quiz, it may present new and " +
                        "different questions to help you with your learning." +
                        "\n\n You can always review your history to previous quiz attempts under each chapter on this page to help you study");
            }
        }
        /*
        There exists a test a student did very poorly on.
        Tests are weighed more than a quiz. So, if there is a test with a 50% score and a quiz with a a 40% score,
        the test will still be considered as "worse"
         */
        else if (worstTestScore >= worstQuizScore) {
            LessonModel.ChapterModel chapter = Main.lessonModel.getChapter(worstTestIndex);
            addText("It looks like your biggest struggle is with ");
            addTextColor("Chapter " + (worstTestIndex + 1) + ": " + chapter.getTitle());
            addText(". You achieved a");
            addTextColor(" test score ");
            addText("of ");
            addTextRed(student.getTest(worstTestIndex).getBestScore() + "%. ");

            HashMap<Integer, Integer> chapterProgresses = new HashMap<>();
            HashMap<Integer, Double> quizScores = new HashMap<>();
            ArrayList<Integer> missedQuizzes = new ArrayList<>();
            boolean hasLowProgress = false;
            boolean missedQuiz = false;
            boolean hasLowQuizScore = false;
            for (int i = worstTestIndex; i >= 0; i--) {
                String chapterTitle = Main.lessonModel.getChapter(i).getTitle();
                /*
                We want to get all of the progresses between this current test and the last test
                Stop once we get beyond to the test test
                 */
                if (i != worstTestIndex && Main.testsModel.hasTest(chapterTitle)) break;

                if (student.getChapter(i).getProgress() < 70) {
                    chapterProgresses.put(i, student.getChapter(i).getProgress());
                    hasLowProgress = true;
                }
                if (student.getQuiz(i).getBestScore() == -1.0) {
                    missedQuiz = true;
                    missedQuizzes.add(i);
                }
                if (Main.quizzesModel.hasQuiz(chapterTitle) && student.getQuiz(i).getBestScore() != -1.0 && student.getQuiz(i).getBestScore() < Configure.MINIMUM_QUIZ_SCORE) {
                    quizScores.put(i, student.getQuiz(i).getBestScore());
                    hasLowQuizScore = true;
                }
            }
            if (hasLowProgress) {
                int numChaptersBeforeTest = chapterProgresses.size();
                addText("Before you took the test, you didn't complete a substantial amount of the reading. ");
                for (Map.Entry<Integer, Integer> iterate : chapterProgresses.entrySet()) {
                    if (iterate.getValue() < 70) {
                        String chapterTitle = Main.lessonModel.getChapter(iterate.getKey()).getTitle();
                        addTextBold("Chapter " + (iterate.getKey() + 1) + ": " + chapterTitle);
                        addText(" was read to ");
                        addText(iterate.getValue() + "% ");
                        numChaptersBeforeTest--;
                        if (numChaptersBeforeTest == 0) {
                            addText("progress. ");
                        } else {
                            addText("progress, ");
                        }
                        if (numChaptersBeforeTest == 1) {
                            addText("and ");
                        }
                    }
                }
                addText("Before continuing, be sure to go back and finish the readings. ");
            }
            if (missedQuiz) {
                if (hasLowProgress) {
                    addTextColor("Additionally, you");
                } else {
                    addTextColor("You");
                }
                addTextColor(" did not even take all of the quizzes leading up to the test. ");
                int numMissedQuizzes = missedQuizzes.size();
                for (int i = missedQuizzes.size() - 1; i >= 0; i--) {
                    String chapterTitle = Main.lessonModel.getChapter(missedQuizzes.get(i)).getTitle();
                    addTextRed("Chapter " + (missedQuizzes.get(i) + 1) + ": " + chapterTitle);
                    numMissedQuizzes--;
                    if (numMissedQuizzes > 0) {
                        addTextRed(", ");
                    }
                    if (numMissedQuizzes == 1) {
                        addText("and ");
                    }
                }
                addText(" were never taken. Make sure to complete all quizzes leading up to the test before taking the test.");
            }
            if (hasLowQuizScore) {
                if (hasLowProgress) {
                    addText("Additionally, you're ");
                } else {
                    addText("You're ");
                }
                addText("performance on the ");
                if (quizScores.size() > 1) {
                    addText("quizzes were");
                } else {
                    addText("quiz was ");
                }
                addText("not as high as it should. ");
                int numQuizzesBeforeTest = quizScores.size();
                for (Map.Entry<Integer, Double> iterate : quizScores.entrySet()) {
                    if (iterate.getValue() == -1.0) continue;
                    String chapterTitle = Main.lessonModel.getChapter(iterate.getKey()).getTitle();
                    addTextBold("Chapter " + (iterate.getKey() + 1) + ": " + chapterTitle + "'s ");
                    addText("quiz score was ");
                    addText(iterate.getValue() + "%");
                    numQuizzesBeforeTest--;
                    if (numQuizzesBeforeTest == 0) {
                        addText(". ");
                    } else {
                        addText(", ");
                    }
                    if (numQuizzesBeforeTest == 1) {
                        addText("and ");
                    }
                }
                addText("For each of the quizzes, review the material presented and try again on the quizzes before " +
                        "trying the final again.");
                addText("\n\nIf you are still not comfortable with the material, it might be necessary to use additional " +
                        "resources or simply practice more. If you were to retake the quiz, it may present new and " +
                        "different questions to help you with your learning." +
                        "\n\n You can always review your history under ");
                addTextBold("Statistics");
                addText(" to previous quiz attempts in helping you study.");

            }
        }

        /*
        Check to see if progress is rather scattered
         */
        int fragmentation = 0;
        for (int i = 0; i < numChapters - 1; i++) {
            /*
            If the progress of the next chapter chapter is sporadic, where each score is low, meaning the student reads a little bit,
            then moves onto the next chapter, we want to catch that.
            However, if the student visited the first page (and only the first page), we ignore that.
             */
            if (student.getChapter(i).getProgress() > (1.0 / student.getChapter(i).getPageVisited().size()) * 100 &&
                    student.getChapter(i).getProgress() < 80 &&
                    student.getChapter(i + 1).getProgress() > (1.0 / student.getChapter(i + 1).getPageVisited().size()) * 100 &&
                    student.getChapter(i + 1).getProgress() < 80) {
                fragmentation++;
            }
        }
        if (fragmentation > 2) {
            addText("\n\n\nIt appears you are working in rather sporadic places. For the purpose of this course, it is best to read " +
                    "in order. Meaning, completely finish one chapter before moving onto the next.");
        }



        for (Text text : texts) {
            subText.getChildren().add(text);
        }
        insights.getChildren().add(subText);
        return insights;
    }

    private static void addText(String string) {
        text = new Text();
        texts.add(text);
        text.setText(string);
        text.getStyleClass().add("box-description");
    }

    private static void addTextBold(String string) {
        text = new Text();
        texts.add(text);
        text.setText(string);
        text.getStyleClass().add("box-description");
        text.setStyle("-fx-font-family: \"Roboto Mono Bold\"; -fx-font-size: 1.1em");
    }

    private static void addTextColor(String string) {
        text = new Text();
        texts.add(text);
        text.setText(string);
        text.getStyleClass().addAll("box-description", "text-color");
        text.setStyle("-fx-font-family: \"Roboto Mono Bold\"; -fx-font-size: 1.1em");
    }

    private static void addTextRed(String string) {
        text = new Text();
        texts.add(text);
        text.setText(string);
        text.getStyleClass().add("box-description");
        text.setStyle("-fx-font-family: \"Roboto Mono Bold\"; -fx-font-size: 1.1em; -fx-fill: #FF1744;"); //Red A400
    }

    private static boolean checkForIncompleteQuiz() {
        //if the student hasn't taken a quiz
        if (!hasTakenQuiz) {
            recentActivityTitle.setText("Before you take your test...");
            addTextBold("\nYou also haven't taken your quiz yet.");
            addText(" Be sure to take it so you are fully prepared for the test!");
            return true;
        }
        return false;
    }

    /**
     * Checks for incomplete progress before taking an exam, and if there is incomplete progress, add text to encourage student to go back and read
     * @return true if there are pages that were not read, and false otherwise
     */
    private static boolean checkForCompleteProgress(String examType) {
        if (progress < 60) {
            recentActivityTitle.setText("Before you take your " + examType + "...");
            if (progress < 40) {
                addText("You haven't read much very material from this chapter. Unless you were already familiar with this material beforehand, it's best that you to continue reading. Pick up at ");
            } else { //then we're between 40% and 60% progress
                addText("You haven't read a considerable amount from this chapter. Unless you were already familiar with this material beforehand, it's best that you to continue reading. Pick up at ");
            }

            //find the earliest page that the student did NOT visit and set newPage to that
            for (int i = 0; i < visitedPages.size(); i++) {
                if (visitedPages.get(i) == 0) {
                    newPage = i;
                    break;
                }
            }
            if (chapter.getPage(newPage) != null && chapter.getPage(newPage).getTitle() != null) {
                addTextBold("page " + (newPage + 1) + ": " + chapter.getPage(newPage).getTitle());
            } else {
                addTextBold("page " + (newPage + 1));
            }
        } else if (progress < 100) {
            recentActivityTitle.setText("Before you take your " + examType + "...");
            if (progress < 80) {
                addText("You haven't quite finished reading the material for this chapter. Unless you were already familiar with this material beforehand, it's best that you to continue reading. You skipped ");
            } else {
                addText("You're so close to finishing reading! Unless you were already familiar with this material beforehand, why not finish up ");
            }


            ArrayList<String> pageTitles = new ArrayList<>();
            for (int i = 0; i < visitedPages.size(); i++) {
                LessonModel.ChapterModel.PageModel page = chapter.getPage(i);
                boolean hasPageTitle2 = page.getTitle() != null;
                //if page not visited
                if (visitedPages.get(i) == 0) {
                    String pageTitle = "page " + (i + 1);
                    if (hasPageTitle2) {
                        pageTitle += ": " + page.getTitle();
                    }
                    pageTitle += ", ";
                    pageTitles.add(pageTitle);
                }
            }

            //the last one is a special case, because we insert a non-bold word "and"
            StringBuilder titles = new StringBuilder();
            for (int i = 0; i < pageTitles.size() - 1; i++) {
                titles.append(pageTitles.get(i));
            }

            addTextBold(titles.toString());

            //make sure we don't stick an "and" in if there's only one page! If that could ever happen...
            if (pageTitles.size() > 1) {
                addText("and ");
            }
            //add the last page, now that we've stuck "and" between the list of pages
            addTextBold(pageTitles.get(pageTitles.size() - 1));

            //a phrase to go with the beginning phrase that is more encouraging and light if the student is close to being done
            if (progress >= 80) {
                addText("real quick?");
            }

            //find the earliest page that the student did NOT visit and set newPage to that
            for (int i = 0; i < visitedPages.size(); i++) {
                if (visitedPages.get(i) == 0) {
                    newPage = i;
                    break;
                }
            }
        }
        return progress == 100;
    }

    public static int getNewPage() {
        return newPage;
    }

    public static int getNewChapter() {
        return newChapter;
    }
}
