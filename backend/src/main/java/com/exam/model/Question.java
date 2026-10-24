package com.exam.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class Question {
    private Integer id;

    @SerializedName("exam_id")
    private Integer examId;

    @SerializedName("question_text")
    private String questionText;

    private Integer marks;

    @SerializedName("negative_marks")
    private Double negativeMarks;

    private List<Option> options = new ArrayList<>();

    public Question() {}

    public Question(Integer id, Integer examId, String questionText, Integer marks, Double negativeMarks) {
        this.id = id;
        this.examId = examId;
        this.questionText = questionText;
        this.marks = marks;
        this.negativeMarks = negativeMarks;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getExamId() {
        return examId;
    }

    public void setExamId(Integer examId) {
        this.examId = examId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public Integer getMarks() {
        return marks;
    }

    public void setMarks(Integer marks) {
        this.marks = marks;
    }

    public Double getNegativeMarks() {
        return negativeMarks;
    }

    public void setNegativeMarks(Double negativeMarks) {
        this.negativeMarks = negativeMarks;
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options = options;
    }
}
