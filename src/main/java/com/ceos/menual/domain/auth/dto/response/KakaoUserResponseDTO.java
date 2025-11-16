package com.ceos.menual.domain.auth.dto.response;

import lombok.Getter;

@Getter
public class KakaoUserResponseDTO {

	private Long id;
	private KakaoAccount kakao_account;

	@Getter
	public static class KakaoAccount {
		private Profile profile;

		@Getter
		public static class Profile {
			private String nickname;
		}
	}
}
