package com.magis.app.models;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class QuizzesModel {

    private Document document;
    private String filePath;
    private ArrayList<ChapterModel> chapters;

    public ChapterModel getChapters(int chapterID) {
        for (ChapterModel chapter : chapters) {
            if (chapterID == chapter.getChapterID()) {
                return chapter;
            }
        }
        return null;
    }

    public QuizzesModel() {
        this.chapters = new ArrayList<>();
        this.filePath = "src/com/magis/app/resources/quizzes.xml";
        File file = new File(filePath);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        try {
            assert dBuilder != null;
            this.document = dBuilder.parse(file);
        } catch (SAXException | IOException e) {
            e.printStackTrace();
        }
        assert this.document != null;
        this.document.getDocumentElement().normalize();

        NodeList chapters = this.document.getElementsByTagName("chapter");
        for (int i = 0; i < chapters.getLength(); i++) {
            Node chapter = chapters.item(i);
            ChapterModel chapterModel = new ChapterModel(chapter);
            this.chapters.add(chapterModel);
        }
    }

    public class ChapterModel {
        private int chapterID;
        private ArrayList<QuestionsModel> questions;

        public ArrayList<QuestionsModel> getQuestions() {
            return questions;
        }

        public int getNumQuestions() {
            return questions.size();
        }

        public int getChapterID() {
            return chapterID;
        }

        ChapterModel(Node chapter) {
            this.chapterID = Integer.parseInt(chapter.getAttributes().getNamedItem("id").getNodeValue());
            this.questions = new ArrayList<>();
            Element chapterElement = (Element) chapter;

            NodeList questions = chapterElement.getElementsByTagName("question");
            for (int i = 0; i < questions.getLength(); i++) {
                Node question = questions.item(i);
                QuestionsModel questionsModel = new QuestionsModel(question);
                this.questions.add(questionsModel);
            }


        }

        public class QuestionsModel {

            private String statement;
            private String correctAnswer;
            private ArrayList<String> incorrectAnswers;

            public String getStatement() {
                return statement;
            }

            public String getCorrectAnswer() {
                return correctAnswer;
            }

            public ArrayList<String> getIncorrectAnswers() {
                return incorrectAnswers;
            }

            QuestionsModel(Node question) {
                this.incorrectAnswers = new ArrayList<>();
                Element questionElement = (Element) question;
                this.statement = questionElement.getElementsByTagName("statement").item(0).getNodeValue();
                NodeList answers = questionElement.getElementsByTagName("answers");
                for (int i = 0; i < answers.getLength(); i++) {
                    Element answer = (Element) answers.item(i);
                    if (answer.hasAttribute("id")) {
                        if (answer.getAttributes().getNamedItem("id").getNodeValue().equals("correct")) {
                            this.correctAnswer = answer.getNodeValue();
                        } else {
                            System.err.println("FAILED to add \"" + answer.getNodeValue() + "\" to list of answer choices. Unknown answer ID with question \"" + statement + "\"");
                        }
                    } else {
                        incorrectAnswers.add(answer.getNodeName());
                    }
                }

            }
        }
    }
}
