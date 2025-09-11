import 'package:flutter/foundation.dart';
class TagDataModel {
  final String tagId;
  final String lastSeenTime;

  TagDataModel({
    required this.tagId,
    required this.lastSeenTime,
  });

  factory TagDataModel.fromJson(Map<Object?, Object?> json) {
    if(kDebugMode){
      return TagDataModel(
      tagId: json['tagId'] as String, //tagId
      lastSeenTime: json['lastSeenTime'] as String, //lastSeenTime
    );

    }else {
      return TagDataModel(
        tagId: json['tagId'] as String, //tagId
        lastSeenTime: json['lastSeenTime'] as String, //lastSeenTime
      );
    }
  }

  Map<String, dynamic> toJson() {
    return kDebugMode?
    {
      'tagId': tagId,
      'lastSeenTime': lastSeenTime,
    }
        :{
      'tagId': tagId,
      'lastSeenTime': lastSeenTime,
    };
  }
}
