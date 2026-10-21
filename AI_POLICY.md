# AI Policy
(Adapted from [LLVM's policy](https://llvm.org/docs/AIToolPolicy.html))
Contributors must read and review all AI-generated code or text before asking maintainers for review, and must be confident that this is a good use of maintainer time (in particular, any code and text should be succinct). They must be able to answer questions about their work during review. The contributor is always the author and is fully accountable for their contributions.

Contributors must be transparent and label contributions that contain substantial amounts of tool-generated content. Contributors should note tool usage in their pull request description, commit message, or wherever authorship is normally indicated for the work.

This policy includes, but is not limited to, the following kinds of contributions:
- Code
- Issues
- Comments and feedback on pull requests

## Details
To ensure sufficient self review and understanding of the work, contributors should write PR descriptions themselves (if needed, using tools for translation or copy-editing). This should explain motivation, implementation approach, expected impact, code coverage, and any open questions or uncertainties to the same extent as a contribution made without tool assistance.

An important implication: agents cannot take action without human approval, such as the GitHub @claude agent. Similarly, automated review tools that publish comments without human review are not allowed. However, an opt-in review tool that keeps a human in the loop is acceptable under this policy.
