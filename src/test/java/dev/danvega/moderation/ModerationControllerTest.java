package dev.danvega.moderation;

import org.junit.jupiter.api.Test;
import org.springframework.ai.decision.DecisionModel;
import org.springframework.ai.decision.DecisionResponse;
import org.springframework.ai.decision.answer.ChoiceAnswer;
import org.springframework.ai.decision.answer.RefusalAnswer;
import org.springframework.ai.decision.answer.ScoreAnswer;
import org.springframework.ai.decision.answer.YesNoAnswer;
import org.springframework.ai.decision.answer.DecisionAnswer;

import dev.danvega.moderation.ModerationController.Action;
import dev.danvega.moderation.ModerationController.Comment;

import static org.assertj.core.api.Assertions.assertThat;

class ModerationControllerTest {

    // A fake Jev, so the tests run without an API key.
    // tone is the level: 0 = Rude, 1 = Neutral, 2 = Kind
    static DecisionModel fakeJev(double spam, String topic, int tone) {
        return prompt -> DecisionResponse.builder(prompt)
                .answer("spam", YesNoAnswer.builder().probability(spam).build())
                .answer("topic", ChoiceAnswer.builder().value(topic).build())
                .answer("tone", ScoreAnswer.builder().value(tone).build())
                .build();
    }

    static Action moderate(DecisionModel jev) {
        return new ModerationController(jev).moderate(new Comment("Any comment")).action();
    }

    @Test
    void publishesAKindQuestion() {
        assertThat(moderate(fakeJev(0.02, "Question", 2))).isEqualTo(Action.PUBLISH);
    }

    @Test
    void rejectsSpam() {
        assertThat(moderate(fakeJev(0.97, "Off topic", 1))).isEqualTo(Action.REJECT);
    }

    @Test
    void holdsMaybeSpamForAPerson() {
        assertThat(moderate(fakeJev(0.6, "Feedback", 2))).isEqualTo(Action.HOLD);
    }

    @Test
    void holdsARudeComment() {
        assertThat(moderate(fakeJev(0.05, "Feedback", 0))).isEqualTo(Action.HOLD);
    }

    @Test
    void holdsWhenJevRefuses() {
        DecisionAnswer refused = RefusalAnswer.builder().reason("Not sure").build();
        DecisionModel jev = prompt -> DecisionResponse.builder(prompt)
                .answer("spam", refused)
                .answer("topic", refused)
                .answer("tone", refused)
                .build();

        var moderation = new ModerationController(jev).moderate(new Comment("Any comment"));

        assertThat(moderation.action()).isEqualTo(Action.HOLD);
        assertThat(moderation.reason()).isEqualTo("Jev did not answer");
    }
}
