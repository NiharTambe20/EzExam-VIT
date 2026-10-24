package com.exam.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class Exam {
    private Integer id;
    private String title;
    private String description;

    @SerializedName("duration_minutes")
    private Integer durationMinutes;

    @SerializedName("total_marks")
    private Integer totalMarks;

    @SerializedName("passing_percentage")
    private Integer passingPercentage;

    @SerializedName("negative_mark")
    private Double negativeMark;

    private String status; // "draft" or "published"

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("end_time")
    private String endTime;

    @SerializedName("created_by")
    private Integer createdBy;

    @SerializedName("question_count")
    private Integer questionCount;

    private List<Question> questions = new ArrayList<>();

    public Exam() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(Integer totalMarks) {
        this.totalMarks = totalMarks;
    }

    public Integer getPassingPercentage() {
        return passingPercentage;
    }

    public void setPassingPercentage(Integer passingPercentage) {
        this.passingPercentage = passingPercentage;
    }

    public Double getNegativeMark() {
        return negativeMark;
    }

    public void setNegativeMark(Double negativeMark) {
        this.negativeMark = negativeMark;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }
}
