package com.ke.bella.workflow.api;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JacksonXmlRootElement(localName = "xml")
public class WechatCallBackBody {
	/**
	 * 机器人主动推送消息的url
	 */
	@JacksonXmlCData
	@JacksonXmlProperty(localName = "WebhookUrl")
	private String webhookUrl;
	/**
	 * 本次回调的唯一性标志，开发者需据此进行事件排重（可能因为网络等原因重复回调）
	 */
	@JacksonXmlCData
	@JacksonXmlProperty(localName = "MsgId")
	private String msgId;
	/**
	 * 会话id，可能是群聊和单聊
	 */
	@JacksonXmlCData
	@JacksonXmlProperty(localName = "ChatId")
	private String chatId;
	/**
	 * 会话类型，single\group，分别表示：单聊\群聊
	 */
	@JacksonXmlProperty(localName = "ChatType")
	private String chatType;
	/**
	 * 该事件触发者的信息
	 */
	@JacksonXmlElementWrapper(localName = "From", useWrapping = false)
	@JacksonXmlProperty(localName = "From")
	private From from;
	/**
	 * 文本(text)。用户群里@机器人或者单聊中向机器人发送文本消息的时候会回调。
	 * 图片（image）。用户在单聊中向机器人发送图片消息的时候会回调。
	 * 图文混排（mixed）。用户群里@机器人或者单聊中向机器人发送图文混排消息的时候会回调。
	 */
	@JacksonXmlProperty(localName = "MsgType")
	private String msgType;
	/**
	 * 事件
	 */
	@JacksonXmlProperty(localName = "Event")
	private Event event;
	/**
	 * 文本
	 */
	@JacksonXmlProperty(localName = "Text")
	private MixedMessage.Text text;
	/**
	 * 图片
	 */
	@JacksonXmlProperty(localName = "Image")
	private MixedMessage.MsgItem.Image image;
	/**
	 * 图文混排
	 */
	@JacksonXmlProperty(localName = "MixedMessage")
	private MixedMessage mixedMessage;
	/**
	 * 一个临时性的webhook地址，开发者可以用该地址回复消息以便响应该事件需注意：24小时内有效且只能使用一次
	 */
	@JacksonXmlCData
	@JacksonXmlProperty(localName = "ResponseUrl")
	private String responseUrl;
	/**
	 * 用户点击的模版卡片提交的数据
	 */
	@JacksonXmlProperty(localName = "TemplateCardEvent")
	private TemplateCardEvent templateCardEvent;

	@Data
	@NoArgsConstructor
	public static class Event {
		/**e
		 * 事件类型：目前可能是add_to_chat表示被添加进会话，或者delete_from_chat表示被移出会话, enter_chat 表示用户进入机器人单聊
		 */
		@JacksonXmlCData
		@JacksonXmlProperty(localName = "EventType")
		private String ventType;
	}

	@Data
	@NoArgsConstructor
	public static class TemplateCardEvent {
		/**
		 * 模版卡片的模版类型
		 */
		@JacksonXmlCData
		@JacksonXmlProperty(localName = "CardType")
		private String cardType;
		/**
		 * 用户点击的提交按钮key
		 */
		@JacksonXmlCData
		@JacksonXmlProperty(localName = "EventKey")
		private String eventKey;
		/**
		 * 用户点击的交互模版卡片的task_id
		 */
		@JacksonXmlCData
		@JacksonXmlProperty(localName = "TaskId")
		private String taskId;
		/**
		 * 用户点击提交的投票选择框数据
		 */
		@JacksonXmlCData
		@JacksonXmlProperty(localName = "SelectedItems")
		private List<SelectedItem> selectedItems;

		@Data
		@NoArgsConstructor
		public static class SelectedItem {
			/**
			 * 用户点击提交的投票选择框的key值
			 */
			@JacksonXmlCData
			@JacksonXmlProperty(localName = "QuestionKey")
			private String questionKey;
			/**
			 * 用户在投票选择框选择的数据。当投票选择框为单选的时候，OptionIds数组只有一个选项key值;
			 */
			@JacksonXmlCData
			@JacksonXmlProperty(localName = "OptionIds")
			private List<OptionId> optionIds;

			@Data
			@NoArgsConstructor
			public static class OptionId {
				/**
				 *  当投票选择框为单选的时候，OptionIds数组可能有多个选项key值
				 */
				@JacksonXmlCData
				@JacksonXmlProperty(localName = "OptionId")
				private String optionId;
			}
		}
	}

	@Data
	@NoArgsConstructor
	public static class From {
		/**
		 * 操作者的加密userid
		 */
		@JacksonXmlCData
		@JacksonXmlProperty(localName = "UserId")
		private String userId;
	}

	@Data
	@NoArgsConstructor
	public static class MixedMessage {
		/**
		 * 图文混排消息中的某一张图片或者一段文字消息
		 */
		@JacksonXmlCData
		@JacksonXmlProperty(localName = "MsgItem")
		private List<MsgItem> msgItems;

		@Data
		@NoArgsConstructor
		public static class MsgItem {
			/**
			 * MsgItem的消息类型，可以是text或者image类型
			 */
			@JacksonXmlCData
			@JacksonXmlProperty(localName = "MsgType")
			private String msgType;
			/**
			 * 文本
			 */
			@JacksonXmlProperty(localName = "Text")
			private Text text;
			/**
			 * 图片
			 */
			@JacksonXmlProperty(localName = "Image")
			private Image image;

			@Data
			@NoArgsConstructor
			public static class Image {
				/**
				 * 图片的url，注意不可在网页引用该图片
				 */
				@JacksonXmlCData
				@JacksonXmlProperty(localName = "ImageUrl")
				private String imageUrl;
			}
		}

		@Data
		@NoArgsConstructor
		public static class Text {
			/**
			 * 消息内容
			 */
			@JacksonXmlCData
			@JacksonXmlProperty(localName = "Content")
			private String content;
		}
	}

}


















