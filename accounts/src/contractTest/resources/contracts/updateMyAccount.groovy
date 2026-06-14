package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should update current user account"
    request {
        method 'PUT'
        url '/accounts/me'
        headers {
            contentType(applicationJson())
        }
        body([
            firstName: "Updated",
            lastName: "Name",
            birthDate: "1995-10-20"
        ])
    }
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
            id: 1,
            login: "test_user",
            firstName: "Updated",
            lastName: "Name",
            birthDate: "1995-10-20",
            amount: 1000.00
        ])
    }
}
