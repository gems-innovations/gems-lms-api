package com.gems.education.infrastructure.driving.rest.mapper;

import com.gems.education.application.command.ContentCommand;
import com.gems.education.application.command.CourseCommand;
import com.gems.education.application.command.LessonCommand;
import com.gems.education.application.command.ModuleCommand;
import com.gems.education.infrastructure.driving.rest.request.CourseRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CourseMapper {
  private CourseMapper() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static CourseCommand toCommand(CourseRequest request) {
    List<ModuleCommand> modules = new ArrayList<>();
    if (request.getModules() != null) {
      modules = request.getModules().stream().map(mReq -> {
        List<LessonCommand> lessons = new ArrayList<>();
        if (mReq.getLessons() != null) {
          lessons = mReq.getLessons().stream().map(lReq -> {
            List<ContentCommand> contents = new ArrayList<>();
            if (lReq.getContents() != null) {
              contents = lReq.getContents().stream()
                .map(cReq -> new ContentCommand(cReq.getType(), cReq.getValue(), cReq.getOrderIndex()))
                .collect(Collectors.toList());
            }
            return new LessonCommand(lReq.getTitle(), lReq.getOrderIndex(), contents);
          }).collect(Collectors.toList());
        }
        return new ModuleCommand(mReq.getTitle(), mReq.getOrderIndex(), lessons);
      }).collect(Collectors.toList());
    }

    return new CourseCommand(
      request.getTitle(),
      request.getDescription(),
      request.getStatus(),
      request.getInstitutionId(),
      modules
    );
  }
}
